# Web 管理頁模組

## 功能位置

管理者登入後，點擊右上角「啟動 Web 管理頁」，系統會啟動本機網址，例如：

```text
http://127.0.0.1:8080/
```

如果 8080 被占用，會自動改用 8081、8082 或 8083。

## 實作方式

本模組使用 Java 內建 `com.sun.net.httpserver.HttpServer`，不需要另外安裝 Node.js、Vue 或 React。

後端檔案：

```text
src/main/java/librarysystem/service/WebDashboardServer.java
```

前端頁面直接由 Java 回傳 HTML，內容包含：

```text
HTML
CSS
JavaScript
Canvas 圖表
Fetch API
```

## Web 頁面功能

1. 總覽卡片
   - 總書數
   - 啟用使用者
   - 目前借閱中
   - 逾期借閱
   - 書評總數
   - 待處理預約
   - 待審核等級申請

2. 視覺化圖表
   - 主題借閱熱度柱狀圖
   - 主題分布圓餅圖

3. 逾期管理
   - 顯示目前逾期未還紀錄
   - 醒目標示逾期列
   - 顯示逾期天數與模擬罰款

4. Web 查詢頁
   - 書籍查詢
   - 使用者查詢
   - 借還紀錄查詢
   - 書評查詢
   - 預約查詢
   - 等級申請查詢

5. 自動搜尋
   - 輸入關鍵字或切換下拉選單後會自動呼叫 API
   - 不需要按搜尋按鈕

## JSON API

```text
/api/stats
/api/subjects
/api/overdue
/api/books?q=Python&status=AVAILABLE
/api/users?q=A12345678&role=VIP&status=ACTIVE
/api/borrows?student=A12345678&name=張家豪&book=Python&status=OVERDUE
/api/reviews?q=書名&rating=5
/api/reservations?q=張家豪&status=WAITING
/api/role-requests?q=VIP&status=PENDING
```

## 與 Swing 介面的關係

Swing 是主要系統操作介面；Web 管理頁是管理者用於 Demo、查詢與報表展示的輔助介面。兩者讀取同一個 SQLite 資料庫 `library-system.db`，因此資料會同步。
