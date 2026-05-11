# 圖書館借還書系統（Java Swing + SQLite）

這是一份可直接作為課程專題 Demo 的完整版本，使用你提供的 `Users.json`、`Books.json`、`Borrow_records.json` 作為系統初始資料來源，並已補齊基礎功能與所有進階功能。

## 已完成功能

### 學生端
- 註冊 / 登入：新註冊帳號預設為 `NORMAL`
- 書籍查詢：可依題名、作者、主題、出版者、ISBN 查詢
- 借書：依使用者等級顯示可選期限
- 還書：自動更新歸還時間，若逾期會顯示逾期天數與模擬罰款
- 個人借還歷史查詢
- 單本書近期借閱紀錄查詢
- 到期提醒與逾期提醒
- 借閱數量限制
- 預約書籍與歸還後通知下一位預約者
- 書評功能：還書後可撰寫 / 更新書評
- 收藏清單：VIP / GOLD / PLATINUM 可用
- 等級申請：可申請升級至 VIP / GOLD / PLATINUM，等待管理者審核

### 管理者端
- 管理者獨立登入
- 查詢所有借還紀錄 / 依學號與書名篩選
- 查看逾期狀態、逾期天數、模擬罰款
- 使用者停權 / 復權
- 直接調整使用者等級：NORMAL / VIP / GOLD / PLATINUM
- 審核使用者等級申請：核准 / 退回
- 書籍新增 / 修改 / 上架 / 下架（下架採軟刪除，不刪除歷史資料）
- 查閱所有書評與預約紀錄
- 主題借閱熱度 Top 10
- 主題借閱統計視覺化：柱狀圖 / 圓餅圖
- Web 報表：管理者可啟動本機 Web dashboard


## 書籍管理 CRUD 說明

管理者端「書籍管理」頁面支援以下操作：

- 新增書籍：填寫表單後按「新增書籍」。
- 查詢 / 讀取書籍：書籍表格會列出所有書籍及上架、借閱狀態。
- 修改書籍：先選取表格中的書籍，按「載入選取書籍到表單」，修改欄位後按「修改書籍」。
- 下架 / 上架：按「切換上架 / 下架」。下架採用軟刪除，只會將 `books.active` 改成 `0`，不會刪除 `books`、`book_isbns`、借閱紀錄、書評、收藏或預約資料。

## 使用者等級規則

| 等級 | 同時借閱上限 | 可選借閱期限 | 預約上限 | 逾期罰款 | 收藏功能 |
|---|---:|---|---:|---:|---|
| NORMAL | 3 本 | 1 / 3 / 7 天 | 3 本 | 5 元/天 | 不可用 |
| VIP | 5 本 | 1 / 3 / 7 / 14 天 | 5 本 | 3 元/天 | 可用 |
| GOLD | 8 本 | 1 / 3 / 7 / 14 / 21 天 | 8 本 | 2 元/天 | 可用 |
| PLATINUM | 10 本 | 1 / 3 / 7 / 14 / 21 / 30 天 | 10 本 | 1 元/天 | 可用 |

## 技術

- Java 17+
- Swing GUI
- SQLite（透過 `sqlite-jdbc`）
- Maven 專案結構
- Java 內建 `HttpServer` 本機 Web 報表
- 自訂 JSON 解析器（不額外依賴第三方 JSON 函式庫）

## 專案結構

```text
library-system/
├─ pom.xml
├─ README.md
├─ data/
│  ├─ Users.json
│  ├─ Books.json
│  └─ Borrow_records.json
├─ docs/
│  └─ db_schema.sql
├─ src/main/resources/seed/
│  ├─ Users.json
│  ├─ Books.json
│  └─ Borrow_records.json
└─ src/main/java/librarysystem/
   ├─ App.java
   ├─ Database.java
   ├─ model/
   ├─ service/
   ├─ ui/
   └─ util/
```

## 執行方式

### 方法 1：使用 IntelliJ IDEA / Eclipse
1. 匯入 Maven 專案
2. 等待 IDE 自動下載 `sqlite-jdbc`
3. 執行 `librarysystem.App`

### 方法 2：使用 Maven
```bash
mvn clean compile
mvn exec:java
```

## 初始資料說明

系統第一次啟動時會自動匯入：

- `Users.json`：20 筆使用者資料
- `Books.json`：200 筆書籍資料
- `Borrow_records.json`：30 筆歷史借閱資料

### 借閱紀錄的時間解析
`Borrow_records.json` 中像 `-45 days`、`1 days`、`0 days` 這類欄位，會以「匯入當下時間」為基準換算成實際日期時間後再寫入資料庫。

### 密碼處理
- 管理者帳號：
  - `admin / admin123`
  - `librarian / lib123`
- `Users.json` 內的使用者密碼會在匯入時轉成 SHA-256 後存入資料庫。
- 也就是說，登入時請輸入 `Users.json` 裡原本那串密碼字串。
- 例如：
  - `A12345678 / 2a9f8e7d6c5b4a3f2e1d9c8b7a`
  - `B87654321 / 8c7d6e5f4a3b2c1d9e8f7a6b5c`

## Web 報表使用方式

1. 使用管理者登入。
2. 點擊右上角「啟動 Web 報表」。
3. 系統會啟動本機網址，例如 `http://127.0.0.1:8080/`。
4. 報表會即時顯示總書數、使用者數、逾期借閱、主題熱度與逾期罰款清單。

## 資料初始化行為

- 第一次啟動時，會依序匯入使用者、書籍、ISBN、借閱紀錄。
- 若偵測到舊版內建 sample data（5 位使用者 / 12 本書 / 8 筆借閱），系統會自動清除後改匯入新的 JSON 初始資料。
- 若你之前已經用舊版專案跑過並產生自己的 `library-system.db`，而且資料不是上述 sample data，建議先刪除 `library-system.db` 再重新啟動，才能完整重匯初始資料。
- 若舊資料庫的 `users.role_level` 只有 NORMAL / VIP CHECK 限制，程式會自動做輕量 migration，改成支援 NORMAL / VIP / GOLD / PLATINUM。

## 注意事項

- 資料庫檔案會在專案執行目錄生成：`library-system.db`
- 若啟動時出現 JDBC 相關錯誤，通常是 Maven 依賴尚未下載完成
- `data/` 目錄的檔案會優先於內建 resources 使用，方便你之後直接替換 JSON 重新匯入

## 管理者端搜尋功能

本版在管理者後台加上多個搜尋列：

- 借還紀錄：可依學號、姓名、書名 / 書籍 ID、借閱狀態搜尋。
- 使用者管理：可依學號、姓名、使用者 ID、權限等級、帳號狀態搜尋。
- 書籍管理：可依書籍 ID、題名、作者、主題、出版者、出版年、版本、資料來源、附註、ISBN 搜尋，並可用狀態篩選上架中、已下架、已借出、可借。
- 書評與預約：書評可依使用者、學號、書名、內容、評分搜尋；預約可依使用者、學號、書名、預約 ID、狀態搜尋。
- 等級申請：可依學號、姓名、申請理由、申請狀態搜尋。

管理者按「清空條件」後會恢復顯示全部資料。
