# Web 版本使用說明

## 🚀 快速開始

### 1. 安裝依賴
```bash
cd library-web
npm install
```

### 2. 複製資料庫
將 Java 版本的 `library-system.db` 複製到 `library-web/` 目錄：
```bash
cp ../library_v5_pohsiangchen/library-system/library-system.db ./
```

### 3. 啟動伺服器
```bash
npm start
```

輸出應該像這樣：
```
╔════════════════════════════════════════════════╗
║ 📚 圖書館借還書系統 (Web 版本)         ║
╚════════════════════════════════════════════════╝

✓ 伺服器已啟動
✓ 訪問地址: http://localhost:3000
✓ 資料庫路徑: /path/to/library-system.db
```

### 4. 開啟瀏覽器
訪問 `http://localhost:3000`

---

## 👤 測試帳號

### 學生端
- **學號**: A12345678
- **密碼**: 2a9f8e7d6c5b4a3f2e1d9c8b7a (或查看 Users.json)

### 管理者端
- **帳號**: admin
- **密碼**: admin123

或

- **帳號**: librarian
- **密碼**: lib123

---

## 🎯 主要功能

### 學生端 (`student.html`)

#### 首頁統計
- 顯示目前借閱、已歸還、逾期未還的統計

#### 瀏覽書籍
- 搜尋書籍（支援書名、作者、主題）
- 查看可借書籍列表
- 借書
- 查看書籍詳細資訊

#### 借還歷史
- 查看所有借閱紀錄
- 顯示借閱狀態
- 標示逾期未還
- 一鍵還書

#### 個人資料
- 查看個人資訊（唯讀）
- 會員等級
- 帳號狀態

### 管理者端 (`admin.html`)

#### 儀表板
- 總書籍數
- 活躍使用者數
- 目前借閱數
- 逾期未還數

#### 書籍管理
- 查看所有書籍
- 搜尋書籍
- 編輯書籍 (功能開發中)
- 切換上架/下架狀態
- 新增書籍 (功能開發中)

#### 使用者管理
- 查看所有使用者
- 按等級篩選
- 按狀態篩選
- 調整使用者等級
- 切換停權/恢復狀態

#### 借還紀錄
- 查看所有借還紀錄
- 搜尋紀錄
- 篩選狀態
- 顯示逾期狀態

---

## 🔌 API 端點

### 認證
- `POST /api/auth/login` - 登入
- `POST /api/auth/register` - 註冊

### 書籍
- `GET /api/books` - 獲取書籍列表
- `GET /api/books/:id` - 獲取書籍詳情
- `POST /api/books` - 新增書籍 (管理者)
- `PUT /api/books/:id` - 修改書籍 (管理者)
- `PUT /api/books/:id/toggle` - 切換上架/下架 (管理者)

### 借閱
- `GET /api/borrows` - 獲取借閱紀錄
- `POST /api/borrows` - 借書
- `PUT /api/borrows/:id` - 還書

### 管理者
- `GET /api/admin/stats` - 獲取統計資料
- `GET /api/admin/users` - 獲取使用者列表
- `PUT /api/admin/users/:id` - 修改使用者

---

## 🎨 頁面結構

```
public/
├── index.html              # 登入頁
├── student.html            # 學生端
├── admin.html              # 管理者端
├── css/
│   ├── style.css           # 全域樣式
│   ├── login.css           # 登入頁樣式
│   ├── student.css         # 學生端樣式
│   └── admin.css           # 管理者端樣式
└── js/
    ├── api.js              # API 函數
    ├── auth.js             # 認證函數
    ├── student.js          # 學生端邏輯
    └── admin.js            # 管理者端邏輯
```

---

## 🛠️ 開發

### 修改樣式
編輯 `public/css/` 中的 CSS 檔案，瀏覽器會自動重新整理。

### 修改邏輯
編輯 `public/js/` 中的 JavaScript 檔案，瀏覽器會自動重新整理。

### 修改後端
編輯 `server.js`，需要重啟伺服器（按 Ctrl+C 然後 `npm start`）。

---

## ⚠️ 常見問題

**Q: 無法連接到伺服器？**  
A: 確保伺服器正在運行（`npm start`）且資料庫檔案存在。

**Q: 登入後頁面空白？**  
A: 打開瀏覽器的開發者工具 (F12) 查看控制台是否有錯誤訊息。

**Q: 資料沒有更新？**  
A: 嘗試硬重新整理（Ctrl+Shift+R）或清除瀏覽器快取。

**Q: 可以同時運行 Java 版本和 Web 版本嗎？**  
A: 可以，但要使用不同的連接埠。Web 版本使用埠 3000，Java 版本使用埠 8080。

---

## 📝 更新日誌

### v1.0.0 (2026-06-13)
- ✅ 基本登入系統
- ✅ 學生端首頁、書籍瀏覽、借還歷史
- ✅ 管理者端儀表板、書籍管理、使用者管理
- ⏳ 書評功能
- ⏳ 預約功能
- ⏳ 等級申請審批
- ⏳ 實時通知

---

## 📞 技術支援

如有問題，請提交 Issue 或 Pull Request。

---

**祝你使用愉快！🎉**
