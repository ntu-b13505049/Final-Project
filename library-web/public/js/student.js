// 學生端主程式

// 頁面初始化
document.addEventListener('DOMContentLoaded', () => {
  requireStudent();
  initializeStudent();
});

/**
 * 初始化學生端
 */
async function initializeStudent() {
  const user = getUser();

  // 設置使用者資訊
  document.getElementById('userName').textContent = user.name || user.student_id;
  document.getElementById('userRole').textContent = getRoleLabel(user.role);

  // 載入初始數據
  await loadBorrowStats();
  await loadBooks();

  // 設置事件監聽
  setupEventListeners();
}

/**
 * 設置事件監聽
 */
function setupEventListeners() {
  // 側邊欄導航
  document.querySelectorAll('.sidebar a').forEach(link => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      const section = link.getAttribute('data-section');
      showSection(section);
      document.querySelectorAll('.sidebar a').forEach(l => l.classList.remove('active'));
      link.classList.add('active');
    });
  });

  // 搜尋書籍
  const searchBtn = document.getElementById('searchBtn');
  if (searchBtn) {
    searchBtn.addEventListener('click', searchBooks);
  }

  // 登出
  document.getElementById('logoutBtn').addEventListener('click', logout);
}

/**
 * 顯示指定章節
 * @param {string} section - 章節名稱
 */
function showSection(section) {
  document.querySelectorAll('.content-section').forEach(el => {
    el.classList.remove('active');
  });
  document.getElementById(section).classList.add('active');
}

/**
 * 獲取角色標籤
 * @param {string} role - 角色代碼
 * @returns {string}
 */
function getRoleLabel(role) {
  const labels = {
    'NORMAL': '普通會員',
    'VIP': 'VIP 會員',
    'GOLD': 'GOLD 會員',
    'PLATINUM': 'PLATINUM 會員'
  };
  return labels[role] || role;
}

/**
 * 載入借閱統計
 */
async function loadBorrowStats() {
  try {
    const user = getUser();
    const records = await getBorrows(user.id);

    const active = records.filter(r => !r.return_date).length;
    const returned = records.filter(r => r.return_date).length;
    const overdue = records.filter(r => !r.return_date && isOverdue(r.due_date)).length;

    document.getElementById('activeBorrows').textContent = active;
    document.getElementById('returnedBooks').textContent = returned;
    document.getElementById('overdueCount').textContent = overdue;
  } catch (error) {
    console.error('載入統計失敗:', error);
    showError('載入統計資訊失敗');
  }
}

/**
 * 載入書籍列表
 */
async function loadBooks(query = '') {
  try {
    const books = await getBooks(query, 'AVAILABLE');
    const container = document.getElementById('booksList');

    if (books.length === 0) {
      container.innerHTML = '<p style="text-align: center; color: #999;">暫無書籍</p>';
      return;
    }

    container.innerHTML = books.map(book => `
      <div class="book-card">
        <div class="book-cover">📖</div>
        <div class="book-info">
          <h3>${book.title}</h3>
          <p><strong>作者:</strong> ${book.author || 'N/A'}</p>
          <p><strong>主題:</strong> ${book.subject || 'N/A'}</p>
          <p class="available">${book.available_count} 本可借</p>
        </div>
        <div class="book-actions">
          <button class="btn btn-primary btn-small" onclick="showBorrowModal(${book.id}, '${book.title}')">借書</button>
          <button class="btn btn-secondary btn-small" onclick="showBookDetails(${book.id})">詳情</button>
        </div>
      </div>
    `).join('');
  } catch (error) {
    console.error('載入書籍失敗:', error);
    showError('載入書籍失敗');
  }
}

/**
 * 搜尋書籍
 */
async function searchBooks() {
  const query = document.getElementById('searchInput')?.value || '';
  await loadBooks(query);
}

/**
 * 顯示借書模態框
 * @param {number} bookId - 書籍 ID
 * @param {string} bookTitle - 書籍標題
 */
function showBorrowModal(bookId, bookTitle) {
  const modal = document.getElementById('borrowModal');
  document.getElementById('borrowBookTitle').textContent = bookTitle;
  document.getElementById('borrowBookId').value = bookId;
  modal.style.display = 'flex';
}

/**
 * 關閉借書模態框
 */
function closeBorrowModal() {
  document.getElementById('borrowModal').style.display = 'none';
}

/**
 * 執行借書
 */
async function confirmBorrow() {
  try {
    const user = getUser();
    const bookId = document.getElementById('borrowBookId').value;
    const days = parseInt(document.getElementById('borrowDays').value);

    if (!days || days < 1) {
      showError('請選擇有效的借閱期限');
      return;
    }

    await borrowBook(user.id, bookId, days);
    showSuccess('借書成功！');
    closeBorrowModal();
    await loadBorrowStats();
    await loadBooks();
    showSection('history');
    await loadHistory();
  } catch (error) {
    console.error('借書失敗:', error);
    showError(error.message || '借書失敗');
  }
}

/**
 * 顯示書籍詳情
 * @param {number} bookId - 書籍 ID
 */
async function showBookDetails(bookId) {
  try {
    const book = await getBookDetails(bookId);
    const details = `
      <h3>${book.title}</h3>
      <p><strong>作者:</strong> ${book.author}</p>
      <p><strong>出版者:</strong> ${book.publisher}</p>
      <p><strong>出版年:</strong> ${book.publish_year}</p>
      <p><strong>主題:</strong> ${book.subject}</p>
      <p><strong>ISBN:</strong> ${book.isbn}</p>
      <p><strong>版本:</strong> ${book.edition}</p>
      <p><strong>備註:</strong> ${book.notes || 'N/A'}</p>
      <p><strong>可借數量:</strong> ${book.available_count}</p>
    `;
    alert(details);
  } catch (error) {
    console.error('獲取書籍詳情失敗:', error);
    showError('無法獲取書籍詳情');
  }
}

/**
 * 載入借閱歷史
 */
async function loadHistory() {
  try {
    const user = getUser();
    const records = await getBorrows(user.id);

    const container = document.getElementById('historyList');

    if (records.length === 0) {
      container.innerHTML = '<p style="text-align: center; color: #999;">暫無借閱紀錄</p>';
      return;
    }

    container.innerHTML = `
      <table class="records-table">
        <thead>
          <tr>
            <th>書籍名稱</th>
            <th>借閱日期</th>
            <th>到期日期</th>
            <th>還書日期</th>
            <th>狀態</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          ${records.map(record => `
            <tr ${isOverdue(record.due_date) && !record.return_date ? 'class="overdue"' : ''}>
              <td>${record.title}</td>
              <td>${formatDate(record.borrow_date)}</td>
              <td>${formatDate(record.due_date)}</td>
              <td>${record.return_date ? formatDate(record.return_date) : '-'}</td>
              <td>
                ${!record.return_date ? `
                  ${isOverdue(record.due_date) ? `
                    <span class="status-badge status-overdue">逾期 (${getOverdueDays(record.due_date)} 天)</span>
                  ` : `
                    <span class="status-badge status-active">借閱中</span>
                  `}
                ` : `
                  <span class="status-badge status-returned">已還書</span>
                `}
              </td>
              <td>
                ${!record.return_date ? `
                  <button class="btn btn-success btn-small" onclick="confirmReturn(${record.id})">還書</button>
                ` : '-'}
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;
  } catch (error) {
    console.error('載入借閱歷史失敗:', error);
    showError('載入借閱歷史失敗');
  }
}

/**
 * 確認還書
 * @param {number} borrowId - 借閱紀錄 ID
 */
async function confirmReturn(borrowId) {
  if (confirm('確認要還書嗎？')) {
    try {
      await returnBook(borrowId);
      showSuccess('還書成功！');
      await loadHistory();
      await loadBorrowStats();
    } catch (error) {
      console.error('還書失敗:', error);
      showError('還書失敗');
    }
  }
}

/**
 * 顯示成功消息
 * @param {string} message - 消息內容
 */
function showSuccess(message) {
  const msg = document.getElementById('successMessage');
  if (msg) {
    msg.textContent = message;
    msg.style.display = 'block';
    setTimeout(() => {
      msg.style.display = 'none';
    }, 3000);
  }
}

/**
 * 顯示錯誤消息
 * @param {string} message - 消息內容
 */
function showError(message) {
  const msg = document.getElementById('errorMessage');
  if (msg) {
    msg.textContent = message;
    msg.style.display = 'block';
    setTimeout(() => {
      msg.style.display = 'none';
    }, 3000);
  }
}

// 點擊模態框外部時關閉
window.addEventListener('click', (e) => {
  const modal = document.getElementById('borrowModal');
  if (e.target === modal) {
    modal.style.display = 'none';
  }
});
