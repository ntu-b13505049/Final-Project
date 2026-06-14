# 介面美化更新說明

本版針對 Swing GUI 與 Web 管理頁進行視覺與操作體驗優化。

## Swing GUI 美化

- 全域改用較現代的 Nimbus Look & Feel。
- 統一主色系：深藍紫漸層、白色卡片、淺灰背景。
- 登入畫面改為大標題漸層橫幅與卡片式登入區。
- 學生端與管理者端標題列改為漸層 Header。
- 按鈕依用途分色：主要操作為紫色、危險/取消操作為紅色、輔助操作為白底灰框。
- 表格列高加大、標題列加粗、逾期資料保留醒目紅色標示。
- 查詢區、統計區、書籍詳細資訊區改成卡片式區塊，減少畫面擁擠感。
- 輸入框、下拉選單、文字區重新設定邊框與內距，Demo 時更清楚。
- 管理者總覽卡片與主題圖表使用彩色視覺化。

## Web 管理頁

Web 管理頁維持 HTML / CSS / JavaScript 實作，並保留自動搜尋、統計卡片、Canvas 圖表與逾期清單功能。

## 主要修改檔案

- `src/main/java/librarysystem/App.java`
- `src/main/java/librarysystem/util/UiUtil.java`
- `src/main/java/librarysystem/ui/LoginFrame.java`
- `src/main/java/librarysystem/ui/UserDashboardFrame.java`
- `src/main/java/librarysystem/ui/AdminDashboardFrame.java`
