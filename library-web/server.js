const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const crypto = require('crypto');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));
app.use(express.static('public'));

// 資料庫連接
const dbPath = process.env.DB_PATH || path.join(__dirname, 'library-system.db');

// 檢查資料庫是否存在
if (!fs.existsSync(dbPath)) {
  console.error(`❌ 錯誤：找不到資料庫檔案 ${dbPath}`);
  console.error('請確保 library-system.db 存在於專案目錄中');
  process.exit(1);
}

const db = new sqlite3.Database(dbPath, (err) => {
  if (err) {
    console.error('資料庫連接失敗:', err);
  } else {
    console.log('✓ 資料庫連接成功');
  }
});

// 工具函數：SHA256 雜湊
function hashPassword(password) {
  return crypto.createHash('sha256').update(password).digest('hex');
}

// 工具函數：執行 SQL 查詢
function dbGet(sql, params = []) {
  return new Promise((resolve, reject) => {
    db.get(sql, params, (err, row) => {
      if (err) reject(err);
      else resolve(row);
    });
  });
}

function dbAll(sql, params = []) {
  return new Promise((resolve, reject) => {
    db.all(sql, params, (err, rows) => {
      if (err) reject(err);
      else resolve(rows);
    });
  });
}

function dbRun(sql, params = []) {
  return new Promise((resolve, reject) => {
    db.run(sql, params, (err) => {
      if (err) reject(err);
      else resolve();
    });
  });
}

// ==================== 認證 API ====================

app.post('/api/auth/login', async (req, res) => {
  try {
    const { username, password, role } = req.body;

    if (!username || !password) {
      return res.status(400).json({ error: '帳號和密碼不能為空' });
    }

    // 管理者登入
    if (role === 'admin') {
      const admins = {
        'admin': hashPassword('admin123'),
        'librarian': hashPassword('lib123')
      };

      if (admins[username] && admins[username] === hashPassword(password)) {
        return res.json({
          success: true,
          user: {
            id: username,
            username: username,
            role: 'ADMIN'
          },
          token: 'admin_' + Date.now()
        });
      } else {
        return res.status(401).json({ error: '管理者帳號或密碼錯誤' });
      }
    }

    // 學生登入
    const student = await dbGet(
      `SELECT user_id, student_no, name, role_level, status
      FROM users
      WHERE student_no = ?`,
      [username]
      );

    if (!student) {
      return res.status(401).json({ error: '帳號不存在' });
    }

    if (student.status === 'SUSPENDED') {
      return res.status(401).json({ error: '您的帳號已被停權' });
    }

    // 查詢密碼
    const user = await dbGet(
      `SELECT password FROM users WHERE user_id = ?`,
      [student.user_id]
    );

    if (!user || user.password !== hashPassword(password)) 

    res.json({
      success: true,
      user: {
        id: student.user_id,
        student_id: student.student_no,
        name: student.name,
        role: student.role_level,
        status: student.status
      },
      token: 'user_' + Date.now() + '_' + student.id
    });
  } catch (err) {
    console.error('登入失敗:', err);
    res.status(500).json({ error: '伺服器錯誤' });
  }
});

app.post('/api/auth/register', async (req, res) => {
  try {
    const { student_id, name, password } = req.body;

    if (!student_id || !name || !password) {
      return res.status(400).json({ error: '所有欄位都必須填寫' });
    }

    // 檢查是否已存在
    const existing = await dbGet(
      `SELECT user_id FROM users WHERE student_no = ?`,
      [student_id]
    );

    if (existing) {
      return res.status(400).json({ error: '學號已存在' });
    }

    // 新增使用者
    await dbRun(
      `INSERT INTO users
      (student_no, name, password, role_level, status, created_at)
      VALUES (?, ?, ?, 'NORMAL', 'ACTIVE', datetime('now'))`,
      [student_id, name, hashPassword(password)]
    );

    res.json({
      success: true,
      message: '註冊成功，請登入'
    });
  } catch (err) {
    console.error('註冊失敗:', err);
    res.status(500).json({ error: '伺服器錯誤' });
  }
});

// ==================== 書籍 API ====================

app.get('/api/books', async (req, res) => {
  try {
    const { q, status } = req.query;
    let sql = `SELECT * FROM books WHERE 1=1`;
    const params = [];

    // 預設只顯示上架的書籍
    sql += ` AND active = 1`;

    if (status === 'AVAILABLE') {
      sql += ` AND available_count > 0`;
    } else if (status === 'UNAVAILABLE') {
      sql += ` AND available_count = 0`;
    }

    if (q) {
      sql += ` AND (title LIKE ? OR author LIKE ? OR subject LIKE ?)`;
      params.push(`%${q}%`, `%${q}%`, `%${q}%`);
    }

    sql += ` ORDER BY title ASC LIMIT 100`;

    const books = await dbAll(sql, params);
    res.json(books);
  } catch (err) {
    console.error('查詢書籍失敗:', err);
    res.status(500).json({ error: '伺服器錯誤' });
  }
});

app.get('/api/books/:id', async (req, res) => {
  try {
    const book = await dbGet(
      `SELECT * FROM books WHERE id = ? AND active = 1`,
      [req.params.id]
    );

    if (!book) {
      return res.status(404).json({ error: '書籍不存在' });
    }

    res.json(book);
  } catch (err) {
    console.error('取得書籍詳細資訊失敗:', err);
    res.status(500).json({ error: '伺服器錯誤' });
  }
});

// ==================== 借閱 API ====================

app.get('/api/borrows', async (req, res) => {
  try {
    const { user_id, status } = req.query;

    if (!user_id) {
      return res.status(400).json({ error: '缺少 user_id 參數' });
    }

    let sql = `
      SELECT b.*, bo.title, bo.author
      FROM borrow_records b
      JOIN books bo ON b.book_id = bo.id
      WHERE b.user_id = ?
    `;
    const params = [user_id];

    if (status === 'ACTIVE') {
      sql += ` AND b.return_date IS NULL`;
    } else if (status === 'RETURNED') {
      sql += ` AND b.return_date IS NOT NULL`;
    } else if (status === 'OVERDUE') {
      sql += ` AND b.return_date IS NULL AND b.due_date < datetime('now')`;
    }

    sql += ` ORDER BY b.borrow_date DESC`;

    const records = await dbAll(sql, params);
    res.json(records);
  } catch (err) {
    console.error('查詢借閱紀錄失敗:', err);
    res.status(500).json({ error: '伺服器錯誤' });
  }
});

app.post('/api/borrows', async (req, res) => {
  try {
    const { user_id, book_id, days } = req.body;

    if (!user_id || !book_id || !days) {
      return res.status(400).json({ error: '缺少必要欄位' });
    }

    // 取得使用者資訊
    const user = await dbGet(`SELECT role_level FROM users WHERE id = ?`, [user_id]);
    if (!user) {
      return res.status(404).json({ error: '使用者不存在' });
    }

    // 檢查借閱數量限制
    const roleLimit = {
      'NORMAL': 3,
      'VIP': 5,
      'GOLD': 8,
      'PLATINUM': 10
    };
    const limit = roleLimit[user.role_level] || 3;

    const currentBorrows = await dbGet(
      `SELECT COUNT(*) as count FROM borrow_records WHERE user_id = ? AND return_date IS NULL`,
      [user_id]
    );

    if (currentBorrows.count >= limit) {
      return res.status(400).json({ error: `您最多只能同時借 ${limit} 本書` });
    }

    // 檢查書籍是否可借
    const book = await dbGet(`SELECT available_count FROM books WHERE id = ?`, [book_id]);
    if (!book || book.available_count <= 0) {
      return res.status(400).json({ error: '書籍暫時無法借閱' });
    }

    // 新增借閱紀錄
    const dueDate = new Date();
    dueDate.setDate(dueDate.getDate() + parseInt(days));

    await dbRun(
      `INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date, created_at)
       VALUES (?, ?, datetime('now'), ?, datetime('now'))`,
      [user_id, book_id, dueDate.toISOString()]
    );

    // 更新可用數量
    await dbRun(`UPDATE books SET available_count = available_count - 1 WHERE id = ?`, [book_id]);

    res.json({ success: true, message: '借書成功' });
  } catch (err) {
    console.error('借書失敗:', err);
    res.status(500).json({ error: '伺服器錯誤' });
  }
});

app.put('/api/borrows/:id', async (req, res) => {
  try {
    const recordId = req.params.id;

    // 取得借閱紀錄
    const record = await dbGet(
      `SELECT book_id FROM borrow_records WHERE id = ?`,
      [recordId]
    );

    if (!record) {
      return res.status(404).json({ error: '借閱紀錄不存在' });
    }

    // 更新還書日期
    await dbRun(
      `UPDATE borrow_records SET return_date = datetime('now') WHERE id = ?`,
      [recordId]
    );

    // 更新可用數量
    await dbRun(
      `UPDATE books SET available_count = available_count + 1 WHERE id = ?`,
      [record.book_id]
    );

    res.json({ success: true, message: '還書成功' });
  } catch (err) {
    console.error('還書失敗:', err);
    res.status(500).json({ error: '伺服器錯誤' });
  }
});

// ==================== 管理者 API ====================

app.get('/api/admin/stats', async (req, res) => {
  try {
    const totalBooks = await dbGet(`SELECT COUNT(*) as count FROM books WHERE active = 1`);
    const activeUsers = await dbGet(`SELECT COUNT(*) as count FROM users WHERE status = 'ACTIVE'`);
    const currentBorrows = await dbGet(
      `SELECT COUNT(*) as count FROM borrow_records WHERE return_date IS NULL`
    );
    const overdueCount = await dbGet(
      `SELECT COUNT(*) as count FROM borrow_records
       WHERE return_date IS NULL AND due_date < datetime('now')`
    );

    res.json({
      total_books: totalBooks.count,
      active_users: activeUsers.count,
      current_borrows: currentBorrows.count,
      overdue_count: overdueCount.count
    });
  } catch (err) {
    console.error('取得統計資訊失敗:', err);
    res.status(500).json({ error: '伺服器錯誤' });
  }
});

app.get('/api/admin/users', async (req, res) => {
  try {
    const { q, role, status } = req.query;
    let sql = `SELECT id, student_id, name, role_level, status FROM users WHERE 1=1`;
    const params = [];

    if (q) {
      sql += ` AND (student_id LIKE ? OR name LIKE ?)`;
      params.push(`%${q}%`, `%${q}%`);
    }

    if (role) {
      sql += ` AND role_level = ?`;
      params.push(role);
    }

    if (status) {
      sql += ` AND status = ?`;
      params.push(status);
    }

    sql += ` ORDER BY student_id ASC LIMIT 100`;

    const users = await dbAll(sql, params);
    res.json(users);
  } catch (err) {
    console.error('查詢使用者失敗:', err);
    res.status(500).json({ error: '伺服器錯誤' });
  }
});

app.put('/api/admin/users/:id', async (req, res) => {
  try {
    const { role_level, status } = req.body;
    const userId = req.params.id;

    let sql = `UPDATE users SET `;
    const params = [];
    const updates = [];

    if (role_level) {
      updates.push(`role_level = ?`);
      params.push(role_level);
    }

    if (status) {
      updates.push(`status = ?`);
      params.push(status);
    }

    if (updates.length === 0) {
      return res.status(400).json({ error: '沒有要更新的欄位' });
    }

    sql += updates.join(', ') + ` WHERE id = ?`;
    params.push(userId);

    await dbRun(sql, params);

    res.json({ success: true, message: '更新成功' });
  } catch (err) {
    console.error('更新使用者失敗:', err);
    res.status(500).json({ error: '伺服器錯誤' });
  }
});

// ==================== 伺服器啟動 ====================

app.listen(PORT, () => {
  console.log(`\n\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557`);
  console.log(`\u2551 \ud83d\udcda \u5716\u66f8\u9928\u501f\u9084\u66f8\u7cfb\u7d71 (Web \u7248\u672c)         \u2551`);
  console.log(`\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255d\n`);
  console.log(`✓ 伺服器已啟動`);
  console.log(`✓ 訪問地址: http://localhost:${PORT}`);
  console.log(`✓ 資料庫路徑: ${dbPath}\n`);
});

module.exports = app;
