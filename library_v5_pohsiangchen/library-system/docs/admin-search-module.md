# 管理者端搜尋功能模組

## 本次新增功能

本次修改主要讓管理者後台不只顯示全部資料，而是能針對不同管理資料表進行搜尋與篩選。

## 新增搜尋頁面

1. 借還紀錄搜尋
   - 學號
   - 姓名
   - 書名 / 書籍 ID
   - 狀態：全部、借閱中、已歸還、逾期、未逾期

2. 使用者搜尋
   - 學號 / 姓名 / 使用者 ID
   - 權限：NORMAL / VIP / GOLD / PLATINUM
   - 狀態：ACTIVE / SUSPENDED

3. 書籍搜尋
   - 書籍 ID
   - 題名
   - 作者
   - 主題
   - 出版者
   - 出版年
   - 版本
   - 資料來源
   - 附註
   - ISBN
   - 狀態：上架中、已下架、已借出、可借

4. 書評搜尋
   - 使用者
   - 學號
   - 書名
   - 書籍 ID
   - 書評內容
   - 評分

5. 預約搜尋
   - 預約 ID
   - 使用者
   - 學號
   - 書名
   - 書籍 ID
   - 狀態：WAITING / NOTIFIED / FULFILLED / CANCELLED

6. 等級申請搜尋
   - 申請 ID
   - 學號
   - 姓名
   - 申請等級
   - 申請理由
   - 管理者備註
   - 狀態：PENDING / APPROVED / REJECTED

## 修改檔案

- `src/main/java/librarysystem/ui/AdminDashboardFrame.java`
- `src/main/java/librarysystem/service/AdminService.java`

## 整合注意事項

- `AdminDashboardFrame.java` 負責畫面與按鈕事件。
- `AdminService.java` 負責實際 SQL 查詢。
- 搜尋功能都支援空條件；空條件會顯示全部資料。
- 「清空條件」會清除欄位並重新載入全部資料。
