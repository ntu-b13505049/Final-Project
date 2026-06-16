# 登入 / 登出視窗尺寸保持

本次修正登入、管理者登入、學生登入與登出流程。

切換視窗時不再重新置中或恢復固定大小，而是透過 `UiUtil.showFrameReplacing(...)` 保留原視窗的：

- 視窗寬度與高度
- 視窗所在位置
- 最大化狀態

因此使用者在登入或登出時，畫面不會突然縮小、放大或跳到螢幕中央。

主要修改檔案：

- `src/main/java/librarysystem/util/UiUtil.java`
- `src/main/java/librarysystem/ui/LoginFrame.java`
- `src/main/java/librarysystem/ui/UserDashboardFrame.java`
- `src/main/java/librarysystem/ui/AdminDashboardFrame.java`
