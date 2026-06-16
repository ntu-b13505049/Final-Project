# 登入 / 登出不縮放視窗修正

## 修正內容

登入、登出與角色介面切換時，系統現在會保留目前視窗的：

- 寬度與高度
- 視窗位置
- 最大化狀態

也就是：

- 登入畫面進入學生介面不會突然縮小或放大
- 登入畫面進入管理者介面不會突然縮小或放大
- 學生登出回登入畫面不會突然縮小或放大
- 管理者登出回登入畫面不會突然縮小或放大

## 技術做法

主要邏輯放在：

```text
src/main/java/librarysystem/util/UiUtil.java
```

核心方法：

```java
UiUtil.replaceWindow(currentFrame, nextFrame)
```

它會在顯示下一個視窗前，先把目前視窗的 bounds 與 extendedState 複製到下一個視窗，因此切換畫面時不會出現視窗尺寸跳動。

## 修改檔案

```text
src/main/java/librarysystem/util/UiUtil.java
src/main/java/librarysystem/ui/LoginFrame.java
src/main/java/librarysystem/ui/UserDashboardFrame.java
src/main/java/librarysystem/ui/AdminDashboardFrame.java
```
