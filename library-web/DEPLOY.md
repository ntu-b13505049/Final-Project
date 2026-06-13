# Web 版本部署指南

## 🚀 部署到 Render

### 前置條件
- GitHub 帳號
- Render 帳號 (https://render.com - 免費註冊)
- 已將程式碼推送到 GitHub

---

## 📋 部署步驟

### Step 1: 登入 Render
1. 訪問 https://render.com
2. 點擊 "Sign up" 用 GitHub 帳號登入
3. 授權 Render 存取你的 GitHub repositories

### Step 2: 建立新 Web Service
1. 在 Render Dashboard 點擊 "New +"
2. 選擇 "Web Service"
3. 連接你的 GitHub repository (`ntu-b13505049/Final-Project`)

### Step 3: 設定部署
在 "Create a new Web Service" 頁面填寫：

**基本設定：**
- Name: `library-system-web`
- Root Directory: `library-web`
- Runtime: `Node`
- Build Command: `npm install`
- Start Command: `npm start`

**環境變數：**
點擊 "Add Environment Variable"，新增：
```
NODE_ENV = production
PORT = 3000
```

### Step 4: 部署
1. 點擊 "Create Web Service"
2. 等待部署完成（通常需要 2-5 分鐘）
3. 部署完成後會顯示你的網址，例如：
   ```
   https://library-system-web.onrender.com
   ```

---

## ⚠️ 重要事項

### 資料庫處理
由於 SQLite 在 Render 的臨時存儲中，每次部署後資料可能會遺失。

**解決方案 1：使用 PostgreSQL**（推薦）
- 在 Render 上建立免費 PostgreSQL 資料庫
- 修改 server.js 使用 PostgreSQL 而非 SQLite

**解決方案 2：定期備份**
- 在本機保存 `library-system.db`
- 定期上傳最新版本

### 冷啟動
Render 免費方案會在 15 分鐘無活動後進入休眠。
重新訪問時需要等待 30 秒左右的啟動時間。

---

## 🔗 部署後的連結

部署成功後，你的網站連結會是：
```
https://library-system-web.onrender.com
```

### 訪問頁面
- 登入: `https://library-system-web.onrender.com`
- 學生端: `https://library-system-web.onrender.com/student.html`
- 管理者端: `https://library-system-web.onrender.com/admin.html`

---

## 🔄 自動部署

設定完成後，每次你推送到 GitHub 的 `web-version` 分支，Render 會自動部署。

### 推送更新
```bash
git add .
git commit -m "更新網站"
git push origin web-version
```

Render 會自動偵測變更並重新部署！

---

## ❌ 常見問題

**Q: 部署失敗，顯示 "Build failed"?**  
A: 檢查 console 日誌，通常是依賴安裝失敗。確保 `package.json` 無誤。

**Q: 網站顯示 "Application Error"?**  
A: 檢查環境變數是否正確設定，或查看 Render 的日誌。

**Q: 資料在部署後遺失?**  
A: 這是 SQLite 在臨時存儲的問題。建議升級到 PostgreSQL 或每次保存資料庫快照。

**Q: 如何看日誌?**  
A: 在 Render Dashboard 點擊你的服務，選擇 "Logs" 標籤。

---

## 💡 其他部署選擇

### Railway (推薦替代)
- 訪問: https://railway.app
- 支持 SQLite 和 PostgreSQL
- 免費方案有 $5 積分

### Glitch
- 訪問: https://glitch.com
- 最簡單的部署方式
- 完全免費

### Vercel
- 訪問: https://vercel.com
- 適合前端
- 支持 Node.js serverless

---

**祝你部署順利！如有問題，查看 Render 官方文件或提交 GitHub Issue。** 🚀
