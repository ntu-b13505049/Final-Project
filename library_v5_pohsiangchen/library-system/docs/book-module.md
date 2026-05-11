# 書籍查詢與 CRUD、下架軟刪除模組

## 負責範圍

本模組負責圖書館系統中的書籍資料管理，包含：

1. 書籍查詢：依題名、作者、主題、出版者、ISBN 查詢。
2. 書籍新增：管理者可新增完整書籍資料與 ISBN。
3. 書籍修改：管理者可選取書籍，載入表單後修改題名、作者、主題、出版者、出版年、版本、格式、資料來源、ISBN、附註。
4. 書籍下架 / 上架：管理者可切換書籍是否上架。
5. 下架軟刪除：下架時只更新 `books.active = 0`，不刪除資料庫紀錄。

## 修改後的 CRUD 對應

| CRUD | 系統功能 |
|---|---|
| Create | 管理者新增書籍 |
| Read | 使用者 / 管理者查詢書籍 |
| Update | 管理者修改書籍資料 |
| Delete | 管理者下架書籍，使用軟刪除 |

## 主要檔案

- `src/main/java/librarysystem/model/Book.java`
- `src/main/java/librarysystem/service/LibraryService.java`
- `src/main/java/librarysystem/service/AdminService.java`
- `src/main/java/librarysystem/ui/AdminDashboardFrame.java`
- `src/main/java/librarysystem/ui/UserDashboardFrame.java`
- `src/main/java/librarysystem/Database.java`
- `docs/db_schema.sql`

## 主要方法

### LibraryService

- `searchBooks(...)`：書籍多條件查詢。
- `getBookById(int bookId)`：取得單本書完整資料。

### AdminService

- `addBook(Book book)`：新增書籍。
- `updateBook(Book book)`：修改書籍資料與 ISBN。
- `toggleBookActive(int bookId, boolean active)`：切換上架 / 下架。

### AdminDashboardFrame

- `addBook()`：按下「新增書籍」後呼叫。
- `loadSelectedBookIntoForm()`：將表格選取書籍載入表單。
- `updateSelectedBook()`：按下「修改書籍」後呼叫。
- `clearBookForm()`：清空表單並回到新增模式。

## 軟刪除邏輯

下架時不要執行：

```sql
DELETE FROM books WHERE book_id = ?;
```

正確作法是：

```sql
UPDATE books SET active = 0 WHERE book_id = ?;
```

這樣可以保留：

- 歷史借閱紀錄
- 書評
- 收藏清單
- 預約紀錄
- 報表統計資料

## 與其他模組的整合關係

其他模組透過 `book_id` 關聯書籍資料：

- 借還書：`borrow_records.book_id -> books.book_id`
- 預約：`reservations.book_id -> books.book_id`
- 書評：`reviews.book_id -> books.book_id`
- 收藏：`favorites.book_id -> books.book_id`
- 報表：使用 `books.subjects` 與 `borrow_records` 做主題借閱統計

下架書籍不可借閱、不可預約，但仍可以在歷史紀錄、書評、收藏與報表中被查到。

## 測試流程

1. 使用管理者登入：`admin / admin123`。
2. 進入「書籍管理」。
3. 填寫表單並按「新增書籍」。
4. 在表格選取該書，按「載入選取書籍到表單」。
5. 修改題名或作者，按「修改書籍」。
6. 按「重新整理」，確認書籍資料已更新。
7. 選取同一本書，按「切換上架 / 下架」。
8. 確認狀態變成「已下架」。
9. 重新上架後，確認狀態回到「上架中 / 可借」。
