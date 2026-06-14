# 健身房會員系統（Java Swing + SQLite + JDBC）

本版已改成 **SQLite**，不用安裝 MySQL Server，也不用開 MySQL Workbench。第一次啟動時，系統會自動在專案資料夾建立本機資料庫檔案：

```text
data/gym_member_system.db
```

專案仍保留 GUI 圖形化介面、資料庫持久化、MVC/OOP 分層、多角色、會員/教練/課程/預約/候補、場館動態人數、器材、儲值、商品販售、健身紀錄與後續追蹤等功能。


## 0. 本次介面與功能更新

- 介面已重新美化：登入頁改為卡片式版面，主畫面加入藍色標題列，表格改為斑馬紋、藍色表頭、圓角卡片、統一按鈕與輸入框樣式。
- 登入與登出會沿用同一個視窗大小與位置，不會從主畫面縮回小登入視窗。
- 登入畫面支援按 Enter 直接登入。
- 各資料表新增「自動搜尋」欄位，輸入關鍵字後會立即篩選，不需要按搜尋按鈕。
- 全系統字體、表格列高與表格欄寬已放大，畫面閱讀更清楚。
- 所有表格都可「雙擊資料列」或選取後按「查看選取詳細」開啟大型詳細視窗，會員、課程、預約、交易、健身紀錄、追蹤建議等都適用。
- 會員端移除儲值操作，只能查看錢包餘額與交易紀錄；儲值由管理員端處理。
- Suspended 會員不可登入，需由管理員改回 Active 才能登入。
- 器材「完成維護(+90天)」改成只更新狀態、上次維護日與下次維護日，避免其他日期被清空。
- 會員追蹤建議改為較完整的大型顯示區，可完整查看目標、目前狀況與教練建議。
- 管理員/教練場館頁新增「目前在場會員」清單，可查看每個場館內有哪些會員。

## 1. 專案結構

```text
gym-member-system/
├─ pom.xml
├─ sql/schema.sql                         # SQLite 建表與示範資料
├─ data/                                  # SQLite 資料庫檔案會自動建立在這裡
├─ scripts/build.sh                       # javac 編譯
├─ scripts/run.sh                         # Linux/macOS 執行
├─ scripts/run.bat                        # Windows 執行
└─ src/main/
   ├─ java/com/gymapp/
   │  ├─ App.java                         # 入口點
   │  ├─ config/                          # SQLite DB 設定
   │  ├─ database/                        # JDBC 連線、初始化、查詢工具
   │  ├─ model/                           # OOP Model：User/Member/Trainer/GymClass/Wallet...
   │  ├─ dao/                             # DAO：資料表 CRUD
   │  ├─ service/                         # 商業邏輯：預約扣點、候補遞補、進出場...
   │  ├─ observer/                        # 候補觀察者模式
   │  ├─ util/                            # 共用工具
   │  └─ view/                            # Swing GUI
   └─ resources/
      ├─ db.properties                    # SQLite 連線設定
      └─ schema.sql                       # classpath 初始化用
```

## 2. SQLite 設定

`src/main/resources/db.properties` 預設內容：

```properties
db.url=jdbc:sqlite:data/gym_member_system.db
```

通常不用改。換電腦執行時，只要整個專案帶過去，第一次執行就會自動建立 `data/gym_member_system.db`。

如果要指定資料庫位置，可改成：

```properties
db.url=jdbc:sqlite:C:/Users/你的帳號/Desktop/gym_member_system.db
```

或用 JVM 參數覆蓋：

```bash
-Ddb.url=jdbc:sqlite:data/gym_member_system.db
```

## 3. 預設登入帳號

| 角色 | 帳號 | 密碼 |
|---|---|---|
| 管理員 | `admin` | `admin123` |
| 教練 | `trainer` | `trainer123` |
| 會員 | `member` | `member123` |

## 4. 執行方式 A：Maven，推薦

先確認電腦有安裝 Java 17 以上與 Maven，然後在專案資料夾執行：

```bash
mvn clean compile exec:java
```

Maven 會自動下載 SQLite JDBC driver：

```text
org.xerial:sqlite-jdbc
```

## 5. 執行方式 B：javac + sqlite-jdbc jar

若不用 Maven，需要自行下載 SQLite JDBC driver，將 jar 放到：

```text
lib/sqlite-jdbc-版本號.jar
```

Linux/macOS：

```bash
chmod +x scripts/build.sh scripts/run.sh
scripts/run.sh
```

Windows：

```bat
scripts\run.bat
```

若沒有放 sqlite-jdbc jar，程式會出現類似 `No suitable driver found for jdbc:sqlite` 的錯誤。

## 6. 資料庫初始化方式

啟動程式後會自動檢查 SQLite 資料庫；登入畫面也有：

```text
建立/檢查 SQLite 資料庫
```

按下後會執行 `schema.sql`，建立所有資料表與示範資料。因為使用 `INSERT OR IGNORE`，重複執行不會把示範資料重複插入。

## 7. 已實作功能

- **GUI 登入與角色權限**：管理員、教練、會員三種角色。
- **會員管理**：新增、修改、刪除、查詢會員資料，含 Active/Suspended 狀態與錢包點數。
- **教練/員工管理**：管理教練、管理員資料與分館。
- **課程管理**：團課與一對一課程，使用 `GymClass` 多型設計。
- **課程預約**：名額檢查、會員狀態檢查、錢包安全扣點、建立交易紀錄。
- **候補機制**：課程額滿時加入 waitlist；取消預約後透過 Observer 自動嘗試遞補。
- **場館動態人數**：依 `access_log` 每位會員最後進/出場紀錄動態計算即時容留人數，並可在場館頁查看目前場內會員。
- **器材管理**：器材狀態、維護日期、下次維護日期。
- **儲值方案**：管理員可依方案或自訂點數儲值，寫入交易紀錄；會員端僅顯示餘額與交易紀錄。
- **商品販售**：商品庫存、銷售紀錄、會員錢包扣點付款。
- **健身狀況紀錄**：體重、體脂、肌肉量、訓練內容與建議。
- **運動執行紀錄**：動作名稱、重量、次數、訓練時間。
- **後續追蹤管理**：目標、目前狀況、下次追蹤日期、教練建議。

## 8. OOP 與架構對應

- 繼承：`User` → `Member`、`Staff`；`Staff` → `Admin`、`Trainer`。
- 封裝：`Wallet.balance` 為 private，只能透過 `deposit()` / `deduct()` 操作。
- 多型：`GymClass` → `GroupClass`、`PersonalTraining`，不同課程類型有不同預約判斷。
- 觀察者模式：`CourseSeatSubject` + `ReservationObserver` + `WaitlistPromotionObserver`，取消預約後通知候補遞補。
- MVC 分層：`view`、`service/controller-like`、`model`、`dao` 分離。

## 9. 換電腦執行注意事項

SQLite 版本不用重新安裝 MySQL。只需要：

1. 安裝 Java 17 以上。
2. 用 Maven 執行 `mvn clean compile exec:java`，或把 sqlite-jdbc jar 放進 `lib/` 後執行 `scripts\run.bat`。
3. 第一次啟動時會自動建立 `data/gym_member_system.db`。

若要重置資料庫，把下面檔案刪掉後重新啟動即可：

```text
data/gym_member_system.db
```

## 本版介面修正
- 表格欄位會依頁面寬度自動分配，避免欄位超出頁面或水平捲軸影響版面。
- 放大檢視視窗可在內容區、欄位、標題與按鈕等位置直接用滑鼠滾輪捲動。
- 商品購買/銷售成功後會立即刷新商品庫存、銷售紀錄、會員餘額、錢包紀錄與其他分頁資料。

## 本版動作紀錄修正
- 會員端「運動執行紀錄」可選取資料列後修改動作名稱、重量與次數。
- 會員端「運動執行紀錄」可刪除選取資料。
- 健身紀錄頁的修改與刪除整合為「修改/刪除選取」一顆按鈕，會依目前選取的是身體紀錄或動作紀錄執行。
- 所有可刪除資料表都支援選取資料列後按 Delete 或 Backspace 快捷鍵刪除；預約紀錄選取後也可用 Delete 或 Backspace 取消預約。
