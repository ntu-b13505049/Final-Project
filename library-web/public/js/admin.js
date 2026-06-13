// 管理者端主程式

// 頁面初始化
document.addEventListener('DOMContentLoaded', () => {
  requireAdmin();
  initializeAdmin();
});

/**
 * 初始化管理者端
 */
async function initializeAdmin() {
  const user = getUser();

  // 設置使用者資訊
  document.getElementById('userName').textContent = user.username || 'Admin';

  // 載入初始數據
  await loadStats();
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
 * 載入統計資訊
 */
async function loadStats() {
  try {
    const stats = await getAdminStats();

    document.getElementById('totalBooks').textContent = stats.total_books;
    document.getElementById('activeUsers').textContent = stats.active_users;
    document.getElementById('currentBorrows').textContent = stats.current_borrows;
    document.getElementById('overdueCount').textContent = stats.overdue_count;
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
    const books = await getBooks(query);
    const container = document.getElementById('booksList');

    if (books.length === 0) {
      container.innerHTML = '<p style="text-align: center; color: #999;">暫無書籍</p>';
      return;
    }

    container.innerHTML = `
      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>書名</th>
            <th>作者</th>
            <th>主題</th>
            <th>可借數量</th>
            <th>狀態</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          ${books.map(book => `
            <tr>
              <td>${book.id}</td>
              <td>${book.title}</td>
              <td>${book.author}</td>
              <td>${book.subject}</td>
              <td>${book.available_count}</td>
              <td>
                ${book.active ? '上架' : '下架'}
              </td>
              <td>
                <button class="btn btn-primary btn-small" onclick="editBook(${book.id})">編輯</button>
                <button class="btn btn-warning btn-small" onclick="toggleBookStatusConfirm(${book.id})">切換狀態</button>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;
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
 * 編輯書籍
 * @param {number} bookId - 書籍 ID
 */
async function editBook(bookId) {
  try {
    const book = await getBookDetails(bookId);
    alert(`編輯書籍功能開發中\n書籍ID: ${bookId}\n書名: ${book.title}`);
  } catch (error) {
    showError('無法獲取書籍資訊');
  }
}

/**
 * 確認切換書籍狀態
 * @param {number} bookId - 書籍 ID
 */
async function toggleBookStatusConfirm(bookId) {
  if (confirm('確認要切換此書籍的上架/下架狀態嗎？')) {
    try {
      await toggleBookStatus(bookId);
      showSuccess('書籍狀態已更新');
      await loadBooks();
    } catch (error) {
      showError('更新書籍狀態失敗');
    }
  }
}

/**
 * 載入使用者列表
 */
async function loadUsers(query = '', role = '', status = '') {
  try {
    const users = await getUsers(query, role, status);
    const container = document.getElementById('usersList');

    if (users.length === 0) {
      container.innerHTML = '<p style="text-align: center; color: #999;">暫無使用者</p>';
      return;
    }

    container.innerHTML = `
      <table class="table">
        <thead>
          <tr>
            <th>學號</th>
            <th>姓名</th>
            <th>等級</th>
            <th>狀態</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          ${users.map(user => `
            <tr>
              <td>${user.student_id}</td>
              <td>${user.name}</td>
              <td><span class="role-badge role-${user.role_level.toLowerCase()}">${user.role_level}</span></td>
              <td><span class="status-badge ${user.status === 'ACTIVE' ? 'status-active' : 'status-suspended'}">${user.status}</span></td>
              <td>
                <button class="btn btn-primary btn-small" onclick="editUserRole(${user.id}, '${user.name}')">調整等級</button>
                <button class="btn btn-danger btn-small" onclick="toggleUserStatus(${user.id}, '${user.status}')">切換狀態</button>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;
  } catch (error) {
    console.error('載入使用者失敗:', error);
    showError('載入使用者失敗');
  }
}

/**
 * 搜尋使用者
 */
async function searchUsers() {
  const query = document.getElementById('userSearchInput')?.value || '';
  const role = document.getElementById('userRoleFilter')?.value || '';
  const status = document.getElementById('userStatusFilter')?.value || '';
  await loadUsers(query, role, status);
}

/**
 * 編輯使用者等級
 * @param {number} userId - 使用者 ID
 * @param {string} userName - 使用者名稱
 */
function editUserRole(userId, userName) {
  const newRole = prompt(`調整 ${userName} 的等級\n\n(NORMAL / VIP / GOLD / PLATINUM)`);
  if (newRole && ['NORMAL', 'VIP', 'GOLD', 'PLATINUM'].includes(newRole.toUpperCase())) {
    updateUserRole(userId, newRole.toUpperCase())
      .then(() => {
        showSuccess('等級更新成功');
        loadUsers();
      })
      .catch(err => showError('等級更新失敗'));
  }
}

/**
 * 切換使用者狀態
 * @param {number} userId - 使用者 ID
 * @param {string} currentStatus - 當前狀態
 */
async function toggleUserStatus(userId, currentStatus) {
  const newStatus = currentStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
  if (confirm(`確認要${newStatus === 'SUSPENDED' ? '停權' : '恢復'}此使用者嗎？`)) {
    try {
      await updateUserStatus(userId, newStatus);
      showSuccess('使用者狀態已更新');
      await loadUsers();
    } catch (error) {
      showError('更新使用者狀態失敗');
    }
  }
}

/**
 * 載入借還紀錄
 */
async function loadBorrowRecords(studentId = '', status = '') {
  try {
    const container = document.getElementById('recordsList');
    container.innerHTML = '<p style="text-align: center; color: #999;">功能開發中...</p>';
  } catch (error) {
    showError('載入借還紀錄失敗');
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
  } else {
    alert(message);
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
  } else {
    alert(message);
  }
}
