# 圖書館借還書系統 - Web 版本

這是將 Java Swing 版本改寫為 Web 應用的版本。

## 技術棧

- **前端**：HTML5 + CSS3 + JavaScript (原生)
- **後端**：Node.js + Express.js
- **資料庫**：SQLite（與 Java 版本共用同一個資料庫）
- **伺服器**：Express 開發伺服器

## 專案結構

```
library-web/
├── package.json
├── server.js                 # Express 伺服器入口
├── README.md
├── public/                   # 靜態前端文件
│   ├── index.html           # 登入頁面
│   ├── student.html         # 學生端頁面
│   ├── admin.html           # 管理者端頁面
│   ├── css/
│   │   ├── style.css        # 全域樣式
│   │   ├── login.css        # 登入頁面樣式
│   │   ├── student.css      # 學生端樣式
│   │   └── admin.css        # 管理者端樣式
│   └── js/
│       ├── api.js           # API 呼叫函數
│       ├── auth.js          # 認證管理
│       ├── student.js       # 學生端邏輯
│       └── admin.js         # 管理者端邏輯
└── database/
    └── (使用 Java 版本的 library-system.db)
```

## 快速開始

### 前置要求
- Node.js 14+ 版本
- npm (通常隨 Node.js 安裝)
- 已有的 SQLite 資料庫 (library-system.db)

### 安裝步驟

1. **進入 web 版本目錄**
   ```bash
   cd library-web
   ```

2. **安裝依賴**
   ```bash
   npm install
   ```

3. **複製資料庫**
   - 將 Java 版本的 `library-system.db` 複製到 `library-web/` 目錄
   ```bash
   cp ../library_v5_pohsiangchen/library-system/library-system.db ./
   ```

4. **啟動伺服器**
   ```bash
   npm start
   ```

5. **開啟瀏覽器**
   - 訪問 `http://localhost:3000`

## 功能說明

### 登入頁面
- 學生登入：輸入學號和密碼
- 管理者登入：使用管理者帳號
  - `admin / admin123`
  - `librarian / lib123`

### 學生端功能
- 📚 書籍查詢：依題名、作者、主題、出版者、ISBN 查詢
- 📖 借書：選擇借閱期限後借書
- 📕 還書：檢視借閱紀錄並還書
- 📝 書評：撰寫和查看書評
- ⭐ 收藏：收藏喜歡的書籍 (VIP 以上等級)
- 🔔 預約：預約暫時無法借閱的書籍
- 👤 個人資訊：查看個人等級和借閱統計

### 管理者端功能
- 📊 儀表板：總覽統計資訊
- 📚 書籍管理：新增、修改、上架/下架書籍
- 👥 使用者管理：查看和調整使用者等級
- 📋 借還紀錄：查詢所有借還紀錄和逾期狀況
- 📝 書評管理：審核和管理書評
- 🔔 預約管理：管理書籍預約
- ✅ 等級申請：審核使用者的等級升級申請

## API 端點

### 認證
- `POST /api/auth/login` - 使用者登入
- `POST /api/auth/logout` - 登出
- `POST /api/auth/register` - 新使用者註冊

### 書籍
- `GET /api/books` - 查詢書籍清單
- `GET /api/books/:id` - 取得書籍詳細資訊
- `POST /api/books` (管理者) - 新增書籍
- `PUT /api/books/:id` (管理者) - 修改書籍
- `PUT /api/books/:id/toggle` (管理者) - 上架/下架書籍

### 借閱
- `GET /api/borrows` - 取得使用者的借閱紀錄
- `POST /api/borrows` - 借書
- `PUT /api/borrows/:id` - 還書

### 書評
- `GET /api/reviews` - 取得書評
- `POST /api/reviews` - 新增或更新書評

### 預約
- `GET /api/reservations` - 取得預約紀錄
- `POST /api/reservations` - 預約書籍
- `DELETE /api/reservations/:id` - 取消預約

### 管理者
- `GET /api/admin/stats` - 取得統計資訊
- `GET /api/admin/users` - 取得所有使用者
- `PUT /api/admin/users/:id` - 調整使用者等級
- `GET /api/admin/role-requests` - 等級申請清單
- `PUT /api/admin/role-requests/:id` - 審核等級申請

## 使用者等級規則

| 等級 | 同時借閱上限 | 可選借閱期限 | 預約上限 | 逾期罰款 | 收藏功能 |
|---|---:|---|---:|---:|---|
| NORMAL | 3 本 | 1 / 3 / 7 天 | 3 本 | 5 元/天 | 不可用 |
| VIP | 5 本 | 1 / 3 / 7 / 14 天 | 5 本 | 3 元/天 | 可用 |
| GOLD | 8 本 | 1 / 3 / 7 / 14 / 21 天 | 8 本 | 2 元/天 | 可用 |
| PLATINUM | 10 本 | 1 / 3 / 7 / 14 / 21 / 30 天 | 10 本 | 1 元/天 | 可用 |

## 注意事項

- 本 Web 版本使用 Node.js Express 作為後端伺服器
- 資料庫使用與 Java 版本相同的 SQLite 檔案
- 可同時運行 Java 版本和 Web 版本，共享同一份資料庫
- 前端完全採用原生 JavaScript 實現，無需額外框架

## 常見問題

**Q: Web 版本和 Java 版本的資料會共用嗎？**  
A: 是的！只要指向同一個 `library-system.db` 資料庫檔案，兩個版本的資料完全同步。

**Q: 可以同時運行兩個版本嗎？**  
A: 可以，但要注意使用不同的埠號。Java 版本預設使用埠 8080，Web 版本使用埠 3000。

**Q: 如何修改埠號？**  
A: 編輯 `server.js` 中的 `const PORT = 3000;` 改為你想要的埠號。

## 開發中的功能

- [ ] 實時通知功能
- [ ] 暗黑模式
- [ ] 行動應用版本
- [ ] 圖表統計頁面優化

## 貢獻

歡迎提交 Issue 和 Pull Request！

## 授權

MIT
