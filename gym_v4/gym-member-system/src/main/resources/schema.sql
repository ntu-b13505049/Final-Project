PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS branch_info (
    branch_id INTEGER PRIMARY KEY AUTOINCREMENT,
    branch_name TEXT NOT NULL,
    max_capacity INTEGER NOT NULL DEFAULT 0,
    current_capacity INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS staff (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    role TEXT NOT NULL,
    name TEXT NOT NULL,
    account TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    phone TEXT,
    specialty TEXT,
    branch_id INTEGER,
    FOREIGN KEY (branch_id) REFERENCES branch_info(branch_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS member_info (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    account TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    phone TEXT,
    email TEXT,
    status TEXT NOT NULL DEFAULT 'Active',
    wallet_balance INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS course_info (
    course_id INTEGER PRIMARY KEY AUTOINCREMENT,
    course_name TEXT NOT NULL,
    course_type TEXT NOT NULL,
    trainer_id INTEGER,
    branch_id INTEGER,
    schedule_time TEXT NOT NULL,
    max_capacity INTEGER NOT NULL DEFAULT 1,
    enrolled_count INTEGER NOT NULL DEFAULT 0,
    points_required INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (trainer_id) REFERENCES staff(id) ON DELETE SET NULL,
    FOREIGN KEY (branch_id) REFERENCES branch_info(branch_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS reservation_history (
    reservation_id INTEGER PRIMARY KEY AUTOINCREMENT,
    member_id INTEGER NOT NULL,
    course_id INTEGER NOT NULL,
    status TEXT NOT NULL,
    points_deducted INTEGER NOT NULL DEFAULT 0,
    created_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member_info(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES course_info(course_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS waitlist (
    waitlist_id INTEGER PRIMARY KEY AUTOINCREMENT,
    course_id INTEGER NOT NULL,
    member_id INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT '候補中',
    created_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (course_id, member_id, status),
    FOREIGN KEY (course_id) REFERENCES course_info(course_id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES member_info(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS access_log (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    member_id INTEGER NOT NULL,
    branch_id INTEGER NOT NULL,
    action TEXT NOT NULL,
    timestamp TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member_info(id) ON DELETE CASCADE,
    FOREIGN KEY (branch_id) REFERENCES branch_info(branch_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transaction_history (
    transaction_id INTEGER PRIMARY KEY AUTOINCREMENT,
    member_id INTEGER NOT NULL,
    type TEXT NOT NULL,
    amount INTEGER NOT NULL,
    timestamp TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note TEXT,
    FOREIGN KEY (member_id) REFERENCES member_info(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS workout_templates (
    template_id INTEGER PRIMARY KEY AUTOINCREMENT,
    member_id INTEGER NOT NULL,
    template_name TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member_info(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS template_details (
    detail_id INTEGER PRIMARY KEY AUTOINCREMENT,
    template_id INTEGER NOT NULL,
    exercise_name TEXT NOT NULL,
    planned_sets INTEGER NOT NULL DEFAULT 0,
    equipment TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (template_id) REFERENCES workout_templates(template_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS workout_logs (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    member_id INTEGER NOT NULL,
    exercise_name TEXT NOT NULL,
    weight REAL,
    reps INTEGER,
    workout_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member_info(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS fitness_records (
    record_id INTEGER PRIMARY KEY AUTOINCREMENT,
    member_id INTEGER NOT NULL,
    trainer_id INTEGER,
    weight_kg REAL,
    body_fat REAL,
    muscle_mass REAL,
    training_content TEXT,
    suggestion TEXT,
    recorded_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member_info(id) ON DELETE CASCADE,
    FOREIGN KEY (trainer_id) REFERENCES staff(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS follow_up_records (
    follow_id INTEGER PRIMARY KEY AUTOINCREMENT,
    member_id INTEGER NOT NULL,
    trainer_id INTEGER,
    goal TEXT,
    current_status TEXT,
    next_follow_date TEXT,
    suggestion TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member_info(id) ON DELETE CASCADE,
    FOREIGN KEY (trainer_id) REFERENCES staff(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS equipment_info (
    equipment_id INTEGER PRIMARY KEY AUTOINCREMENT,
    equipment_name TEXT NOT NULL,
    type TEXT,
    status TEXT NOT NULL DEFAULT '正常',
    purchase_date TEXT,
    last_maintenance_date TEXT,
    next_maintenance_date TEXT,
    branch_id INTEGER,
    notes TEXT,
    FOREIGN KEY (branch_id) REFERENCES branch_info(branch_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS recharge_plans (
    plan_id INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_name TEXT NOT NULL,
    pay_amount INTEGER NOT NULL,
    points INTEGER NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS products (
    product_id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_name TEXT NOT NULL,
    category TEXT,
    unit_price INTEGER NOT NULL DEFAULT 0,
    stock INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sales_history (
    sale_id INTEGER PRIMARY KEY AUTOINCREMENT,
    member_id INTEGER,
    product_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    total_amount INTEGER NOT NULL,
    sold_by INTEGER,
    timestamp TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member_info(id) ON DELETE SET NULL,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    FOREIGN KEY (sold_by) REFERENCES staff(id) ON DELETE SET NULL
);

INSERT OR IGNORE INTO branch_info (branch_id, branch_name, max_capacity, current_capacity) VALUES
(1, '台北信義館', 80, 0),
(2, '新北板橋館', 60, 0),
(3, '桃園中壢館', 50, 0);

INSERT OR IGNORE INTO staff (id, role, name, account, password, phone, specialty, branch_id) VALUES
(1, '管理員', '系統管理員', 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '0900-000-001', '營運管理', 1),
(2, '教練', '王小明', 'trainer', '5b3d264e4cdc2c39ca6708b3e1e21f082722be12e63ee21484bdbe15735ab066', '0912-345-678', '重量訓練', 1),
(3, '教練', '林小美', 'trainer2', '5b3d264e4cdc2c39ca6708b3e1e21f082722be12e63ee21484bdbe15735ab066', '0922-111-222', '瑜珈伸展', 2);

INSERT OR IGNORE INTO member_info (id, name, account, password, phone, email, status, wallet_balance) VALUES
(1, '李小華', 'member', '5600376e863d2f57a053518f324ad3840b0bc2348b573af281a7b7cbe7a228c6', '0911-222-333', 'member@example.com', 'Active', 1200),
(2, '張小安', 'member2', 'c235630b2dfbf1f4fd73d64e01e3cc66d13a7f358b01e200e83cb607083fb195', '0922-333-444', 'member2@example.com', 'Active', 600),
(3, '停權會員', 'suspended', '5600376e863d2f57a053518f324ad3840b0bc2348b573af281a7b7cbe7a228c6', '0933-444-555', 'stop@example.com', 'Suspended', 500);

INSERT OR IGNORE INTO course_info (course_id, course_name, course_type, trainer_id, branch_id, schedule_time, max_capacity, enrolled_count, points_required) VALUES
(1, '燃脂有氧', '團課', 2, 1, datetime('now', '+1 day'), 20, 0, 120),
(2, '一對一重量訓練', '一對一', 2, 1, datetime('now', '+2 day'), 1, 0, 300),
(3, '晨間瑜珈', '團課', 3, 2, datetime('now', '+3 day'), 15, 0, 100);

INSERT OR IGNORE INTO equipment_info (equipment_id, equipment_name, type, status, purchase_date, last_maintenance_date, next_maintenance_date, branch_id, notes) VALUES
(1, '跑步機 A1', '有氧', '正常', '2023-05-01', '2026-01-10', '2026-04-10', 1, '靠窗第一台'),
(2, '深蹲架 B1', '重量訓練', '維修中', '2022-09-12', '2026-02-01', '2026-03-01', 1, '安全扣待更換'),
(3, '瑜珈墊 20 入', '課程用品', '正常', '2024-01-20', '2026-02-15', '2026-05-15', 2, '團課教室');

INSERT OR IGNORE INTO recharge_plans (plan_id, plan_name, pay_amount, points, description) VALUES
(1, '體驗方案', 500, 500, '適合新會員體驗'),
(2, '月費方案', 1200, 1350, '加贈 150 點'),
(3, '季費方案', 3000, 3600, '加贈 600 點'),
(4, '年費方案', 10000, 13000, '加贈 3000 點');

INSERT OR IGNORE INTO products (product_id, product_name, category, unit_price, stock) VALUES
(1, '蛋白粉', '補給品', 1200, 15),
(2, '運動飲料', '飲料', 35, 40),
(3, '毛巾', '用品', 150, 25),
(4, '乳清隨手包', '補給品', 80, 60);

INSERT OR IGNORE INTO transaction_history (transaction_id, member_id, type, amount, timestamp, note) VALUES
(1, 1, '開卡贈點', 1200, CURRENT_TIMESTAMP, '示範資料'),
(2, 2, '開卡贈點', 600, CURRENT_TIMESTAMP, '示範資料');

INSERT OR IGNORE INTO fitness_records (record_id, member_id, trainer_id, weight_kg, body_fat, muscle_mass, training_content, suggestion, recorded_at) VALUES
(1, 1, 2, 72.5, 18.2, 32.0, '胸推 3 組、划船 3 組、跑步 20 分鐘', '下次增加伸展與核心訓練', CURRENT_TIMESTAMP);

INSERT OR IGNORE INTO follow_up_records (follow_id, member_id, trainer_id, goal, current_status, next_follow_date, suggestion, created_at) VALUES
(1, 1, 2, '三個月減脂 3 公斤', '目前體脂下降 0.8%，飲食控制穩定', date('now', '+14 day'), '維持每週三次訓練並補充蛋白質', CURRENT_TIMESTAMP);
