# 登入 / 登出視窗大小固定改善

## 修改目的

登入、登出時不要重新置中或縮放視窗。

## 做法

新增 `UiUtil.showFrameReplacing(currentFrame, nextFrame)`，在切換 LoginFrame、UserDashboardFrame、AdminDashboardFrame 時，會複製目前視窗的 bounds 與 maximized state。

## 套用位置

- 學生登入：LoginFrame -> UserDashboardFrame
- 管理者登入：LoginFrame -> AdminDashboardFrame
- 學生登出：UserDashboardFrame -> LoginFrame
- 管理者登出：AdminDashboardFrame -> LoginFrame

## 效果

使用者如果把視窗調整成某個大小，或最大化視窗，登入 / 登出後會保持相同大小與最大化狀態，不會突然縮放。
