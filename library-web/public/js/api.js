// API 基礎 URL
const API_BASE_URL = 'http://localhost:3000/api';

// 通用 API 呼叫函數
async function apiCall(endpoint, method = 'GET', data = null) {
  const options = {
    method,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token') || ''}`
    }
  };

  if (data) {
    options.body = JSON.stringify(data);
  }

  try {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, options);
    const result = await response.json();

    if (!response.ok) {
      throw new Error(result.error || '請求失敗');
    }

    return result;
  } catch (error) {
    console.error('API 錯誤:', error);
    throw error;
  }
}

// ==================== 書籍 API ====================

/**
 * 獲取書籍列表
 * @param {string} query - 搜尋關鍵字
 * @param {string} status - 書籍狀態 (AVAILABLE, UNAVAILABLE)
 * @returns {Promise<Array>}
 */
async function getBooks(query = '', status = '') {
  let url = '/books';
  const params = [];

  if (query) params.push(`q=${encodeURIComponent(query)}`);
  if (status) params.push(`status=${status}`);

  if (params.length > 0) {
    url += '?' + params.join('&');
  }

  return apiCall(url);
}

/**
 * 獲取單本書詳細資訊
 * @param {number} bookId - 書籍 ID
 * @returns {Promise<Object>}
 */
async function getBookDetails(bookId) {
  return apiCall(`/books/${bookId}`);
}

/**
 * 新增書籍（管理者）
 * @param {Object} bookData - 書籍資訊
 * @returns {Promise<Object>}
 */
async function addBook(bookData) {
  return apiCall('/books', 'POST', bookData);
}

/**
 * 修改書籍（管理者）
 * @param {number} bookId - 書籍 ID
 * @param {Object} bookData - 書籍資訊
 * @returns {Promise<Object>}
 */
async function updateBook(bookId, bookData) {
  return apiCall(`/books/${bookId}`, 'PUT', bookData);
}

/**
 * 切換書籍上架/下架（管理者）
 * @param {number} bookId - 書籍 ID
 * @returns {Promise<Object>}
 */
async function toggleBookStatus(bookId) {
  return apiCall(`/books/${bookId}/toggle`, 'PUT');
}

// ==================== 借閱 API ====================

/**
 * 獲取借閱紀錄
 * @param {string} userId - 使用者 ID
 * @param {string} status - 狀態 (ACTIVE, RETURNED, OVERDUE)
 * @returns {Promise<Array>}
 */
async function getBorrows(userId, status = '') {
  let url = `/borrows?user_id=${userId}`;
  if (status) url += `&status=${status}`;
  return apiCall(url);
}

/**
 * 借書
 * @param {number} userId - 使用者 ID
 * @param {number} bookId - 書籍 ID
 * @param {number} days - 借閱天數
 * @returns {Promise<Object>}
 */
async function borrowBook(userId, bookId, days) {
  return apiCall('/borrows', 'POST', {
    user_id: userId,
    book_id: bookId,
    days
  });
}

/**
 * 還書
 * @param {number} borrowId - 借閱紀錄 ID
 * @returns {Promise<Object>}
 */
async function returnBook(borrowId) {
  return apiCall(`/borrows/${borrowId}`, 'PUT');
}

// ==================== 管理者 API ====================

/**
 * 獲取統計資訊（管理者）
 * @returns {Promise<Object>}
 */
async function getAdminStats() {
  return apiCall('/admin/stats');
}

/**
 * 獲取所有使用者（管理者）
 * @param {string} query - 搜尋關鍵字
 * @param {string} role - 角色篩選
 * @param {string} status - 狀態篩選
 * @returns {Promise<Array>}
 */
async function getUsers(query = '', role = '', status = '') {
  let url = '/admin/users';
  const params = [];

  if (query) params.push(`q=${encodeURIComponent(query)}`);
  if (role) params.push(`role=${role}`);
  if (status) params.push(`status=${status}`);

  if (params.length > 0) {
    url += '?' + params.join('&');
  }

  return apiCall(url);
}

/**
 * 更新使用者（管理者）
 * @param {number} userId - 使用者 ID
 * @param {Object} userData - 更新資訊
 * @returns {Promise<Object>}
 */
async function updateUser(userId, userData) {
  return apiCall(`/admin/users/${userId}`, 'PUT', userData);
}

/**
 * 調整使用者等級（管理者）
 * @param {number} userId - 使用者 ID
 * @param {string} role_level - 新等級
 * @returns {Promise<Object>}
 */
async function updateUserRole(userId, role_level) {
  return updateUser(userId, { role_level });
}

/**
 * 暫停/恢復使用者（管理者）
 * @param {number} userId - 使用者 ID
 * @param {string} status - 狀態 (ACTIVE, SUSPENDED)
 * @returns {Promise<Object>}
 */
async function updateUserStatus(userId, status) {
  return updateUser(userId, { status });
}

// ==================== 工具函數 ====================

/**
 * 格式化日期
 * @param {string} dateStr - 日期字符串
 * @returns {string} 格式化後的日期
 */
function formatDate(dateStr) {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  return date.toLocaleDateString('zh-TW');
}

/**
 * 計算天數差
 * @param {string} startDate - 開始日期
 * @param {string} endDate - 結束日期
 * @returns {number} 天數差
 */
function getDaysDifference(startDate, endDate) {
  const start = new Date(startDate);
  const end = new Date(endDate);
  const diffTime = Math.abs(end - start);
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
}

/**
 * 檢查是否逾期
 * @param {string} dueDate - 到期日期
 * @returns {boolean}
 */
function isOverdue(dueDate) {
  return new Date(dueDate) < new Date();
}

/**
 * 計算逾期天數
 * @param {string} dueDate - 到期日期
 * @returns {number}
 */
function getOverdueDays(dueDate) {
  if (!isOverdue(dueDate)) return 0;
  return getDaysDifference(dueDate, new Date());
}

/**
 * 計算罰款
 * @param {number} days - 逾期天數
 * @param {number} finePerDay - 每天罰款金額
 * @returns {number}
 */
function calculateFine(days, finePerDay = 5) {
  return days * finePerDay;
}
