# 使用者查詢頁版面修正

## 修正內容
- 修正使用者端「查詢與借書」頁面中，查詢按鈕與「主題」輸入框重疊的問題。
- 將查詢條件區由單一 GridBagLayout 改成外層 BorderLayout：
  - 中間：查詢欄位 GridBagLayout
  - 右側：查詢 / 清空條件按鈕

## 修改檔案
- src/main/java/librarysystem/ui/UserDashboardFrame.java
