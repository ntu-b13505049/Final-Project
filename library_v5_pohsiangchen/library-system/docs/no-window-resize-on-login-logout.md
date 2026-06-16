# 登入 / 登出視窗大小固定修正

## 修改目的

修正登入、登出、切換學生介面與管理者介面時，視窗會重新套用預設大小或重新置中的問題。

## 修改內容

新增 `UiUtil` 的視窗狀態記憶機制：

- `setupStableMainWindow(JFrame frame)`：初次開啟使用預設大小；後續開啟會沿用上一個視窗的位置、大小與最大化狀態。
- `rememberWindowState(JFrame frame)`：切換畫面前記錄目前視窗狀態。
- `switchFrame(JFrame currentFrame, JFrame nextFrame)`：用於登入 / 登出切換，避免新視窗縮放或重新置中。

## 影響檔案

- `src/main/java/librarysystem/util/UiUtil.java`
- `src/main/java/librarysystem/ui/LoginFrame.java`
- `src/main/java/librarysystem/ui/UserDashboardFrame.java`
- `src/main/java/librarysystem/ui/AdminDashboardFrame.java`

## 測試方式

1. 開啟系統。
2. 調整登入視窗大小或最大化。
3. 登入學生或管理者帳號。
4. 確認新介面維持相同視窗大小與最大化狀態。
5. 登出。
6. 確認回到登入畫面時仍維持相同視窗大小與最大化狀態。
