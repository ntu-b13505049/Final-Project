# 已被預約書籍狀態顯示修正

本版修正書籍狀態判斷：如果一本書目前沒有被借出，但已有 `WAITING` 或 `NOTIFIED` 的有效預約紀錄，書籍查詢結果會顯示「已被預約」，不再顯示「可借閱」。

## 判斷優先順序

1. `books.active = 0`：顯示「下架」
2. 有未歸還借閱紀錄：顯示「已借出」；若同時有人預約，顯示「已借出 / 已被預約」
3. 沒有借出但有有效預約：顯示「已被預約」
4. 沒有借出且沒有有效預約：顯示「可借閱」

有效預約狀態包含：

```text
WAITING
NOTIFIED
```

已完成、已取消的預約不會影響書籍可借狀態：

```text
FULFILLED
CANCELLED
```

## 影響範圍

- 學生端書籍查詢結果
- 學生端書籍詳細資訊
- 學生端收藏清單
- 管理者端書籍管理表格
- Web 管理頁書籍查詢
- 管理者端與 Web 管理頁的「可借」篩選會排除已被預約書籍
- 管理者端與 Web 管理頁新增「已被預約」狀態篩選

## 主要修改檔案

- `src/main/java/librarysystem/model/Book.java`
- `src/main/java/librarysystem/service/LibraryService.java`
- `src/main/java/librarysystem/service/AdminService.java`
- `src/main/java/librarysystem/ui/AdminDashboardFrame.java`
- `src/main/java/librarysystem/service/WebDashboardServer.java`
- `README.md`
