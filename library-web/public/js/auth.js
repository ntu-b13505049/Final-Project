// 認證相關函數

/**
 * 保存使用者資訊到 localStorage
 * @param {Object} user - 使用者物件
 * @param {string} token - 認證令牌
 */
function saveUser(user, token) {
  localStorage.setItem('user', JSON.stringify(user));
  localStorage.setItem('token', token);
}

/**
 * 從 localStorage 獲取使用者資訊
 * @returns {Object|null}
 */
function getUser() {
  const user = localStorage.getItem('user');
  return user ? JSON.parse(user) : null;
}

/**
 * 從 localStorage 獲取認證令牌
 * @returns {string}
 */
function getToken() {
  return localStorage.getItem('token') || '';
}

/**
 * 檢查使用者是否已登入
 * @returns {boolean}
 */
function isLoggedIn() {
  return !!getUser() && !!getToken();
}

/**
 * 檢查使用者是否為管理者
 * @returns {boolean}
 */
function isAdmin() {
  const user = getUser();
  return user && user.role === 'ADMIN';
}

/**
 * 檢查使用者是否為學生
 * @returns {boolean}
 */
function isStudent() {
  const user = getUser();
  return user && user.role !== 'ADMIN';
}

/**
 * 登出使用者
 */
function logout() {
  localStorage.removeItem('user');
  localStorage.removeItem('token');
  window.location.href = 'index.html';
}

/**
 * 驗證用戶登入狀態，未登入則重定向到登入頁
 */
function requireLogin() {
  if (!isLoggedIn()) {
    window.location.href = 'index.html';
  }
}

/**
 * 驗證用戶是管理者，否則重定向
 */
function requireAdmin() {
  if (!isAdmin()) {
    window.location.href = 'index.html';
  }
}

/**
 * 驗證用戶是學生，否則重定向
 */
function requireStudent() {
  if (!isStudent()) {
    window.location.href = 'index.html';
  }
}
