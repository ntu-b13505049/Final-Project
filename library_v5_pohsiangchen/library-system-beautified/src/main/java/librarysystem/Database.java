package librarysystem;

import librarysystem.model.RolePolicy;
import librarysystem.util.DateUtil;
import librarysystem.util.SecurityUtil;
import librarysystem.util.SimpleJsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Database {
    private static final String DB_URL = "jdbc:sqlite:library-system.db";
    private static final String USERS_FILE = "Users.json";
    private static final String BOOKS_FILE = "Books.json";
    private static final String BORROW_RECORDS_FILE = "Borrow_records.json";

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public static void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("找不到 SQLite JDBC Driver，請確認 pom.xml 已下載依賴。若你在 IDE 執行，先重新載入 Maven 專案。\n" + e.getMessage());
        }

        try (Connection connection = getConnection()) {
            createTables(connection);
            migrateUsersRoleLevelConstraint(connection);
            seedData(connection);
        } catch (SQLException e) {
            throw new RuntimeException("初始化資料庫失敗", e);
        }
    }

    private static void createTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        student_no TEXT NOT NULL UNIQUE,
                        name TEXT NOT NULL,
                        password TEXT NOT NULL,
                        role_level TEXT NOT NULL,
                        created_at TEXT NOT NULL,
                        status TEXT NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED'))
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS admins (
                        admin_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL UNIQUE,
                        password TEXT NOT NULL,
                        created_at TEXT NOT NULL
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS books (
                        book_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        authors TEXT NOT NULL,
                        subjects TEXT,
                        publisher TEXT,
                        publish_year TEXT,
                        edition TEXT,
                        format_desc TEXT,
                        source TEXT,
                        note TEXT,
                        active INTEGER NOT NULL DEFAULT 1
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS book_isbns (
                        isbn_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        book_id INTEGER NOT NULL,
                        isbn TEXT NOT NULL,
                        FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS borrow_records (
                        record_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        book_id INTEGER NOT NULL,
                        borrow_date TEXT NOT NULL,
                        due_date TEXT NOT NULL,
                        return_date TEXT,
                        borrow_days INTEGER NOT NULL,
                        created_at TEXT NOT NULL,
                        FOREIGN KEY (user_id) REFERENCES users(user_id),
                        FOREIGN KEY (book_id) REFERENCES books(book_id)
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS reviews (
                        review_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        book_id INTEGER NOT NULL,
                        rating INTEGER NOT NULL,
                        content TEXT NOT NULL,
                        created_at TEXT NOT NULL,
                        UNIQUE(user_id, book_id),
                        FOREIGN KEY (user_id) REFERENCES users(user_id),
                        FOREIGN KEY (book_id) REFERENCES books(book_id)
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS favorites (
                        favorite_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        book_id INTEGER NOT NULL,
                        created_at TEXT NOT NULL,
                        UNIQUE(user_id, book_id),
                        FOREIGN KEY (user_id) REFERENCES users(user_id),
                        FOREIGN KEY (book_id) REFERENCES books(book_id)
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        reservation_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        book_id INTEGER NOT NULL,
                        status TEXT NOT NULL CHECK (status IN ('WAITING', 'NOTIFIED', 'FULFILLED', 'CANCELLED')),
                        created_at TEXT NOT NULL,
                        notified_at TEXT,
                        FOREIGN KEY (user_id) REFERENCES users(user_id),
                        FOREIGN KEY (book_id) REFERENCES books(book_id)
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS role_change_requests (
                        request_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        target_level TEXT NOT NULL,
                        reason TEXT,
                        status TEXT NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
                        created_at TEXT NOT NULL,
                        handled_at TEXT,
                        handled_by TEXT,
                        admin_note TEXT,
                        FOREIGN KEY (user_id) REFERENCES users(user_id)
                    )
                    """);
        }
    }

    private static void migrateUsersRoleLevelConstraint(Connection connection) throws SQLException {
        String createSql = null;
        try (PreparedStatement ps = connection.prepareStatement("SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'users'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                createSql = rs.getString(1);
            }
        }

        if (createSql == null || !createSql.contains("role_level IN ('NORMAL', 'VIP')")) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = OFF");
            statement.executeUpdate("DROP TABLE IF EXISTS users_migrated");
            statement.executeUpdate("""
                    CREATE TABLE users_migrated (
                        user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        student_no TEXT NOT NULL UNIQUE,
                        name TEXT NOT NULL,
                        password TEXT NOT NULL,
                        role_level TEXT NOT NULL,
                        created_at TEXT NOT NULL,
                        status TEXT NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED'))
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO users_migrated(user_id, student_no, name, password, role_level, created_at, status)
                    SELECT user_id, student_no, name, password, role_level, created_at, status FROM users
                    """);
            statement.executeUpdate("DROP TABLE users");
            statement.executeUpdate("ALTER TABLE users_migrated RENAME TO users");
            statement.execute("PRAGMA foreign_keys = ON");
        }
    }

    private static void seedData(Connection connection) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            if (count(connection, "admins") == 0) {
                insertAdmin(connection, "admin", "admin123");
                insertAdmin(connection, "librarian", "lib123");
            }

            if (looksLikeLegacySampleData(connection)) {
                clearLibrarySeedData(connection);
            }

            int userCount = count(connection, "users");
            int bookCount = count(connection, "books");
            int borrowRecordCount = count(connection, "borrow_records");

            if (userCount == 0 && bookCount == 0 && borrowRecordCount == 0) {
                importInitialJsonData(connection);
            } else if (userCount == 0 || bookCount == 0 || borrowRecordCount == 0) {
                throw new IllegalStateException("資料庫出現部分初始化狀態，請刪除 library-system.db 後重新啟動。");
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } catch (IOException e) {
            connection.rollback();
            throw new RuntimeException("讀取初始 JSON 資料失敗：" + e.getMessage(), e);
        } catch (RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private static boolean looksLikeLegacySampleData(Connection connection) throws SQLException {
        return count(connection, "users") == 5
                && count(connection, "books") == 12
                && count(connection, "borrow_records") == 8
                && exists(connection, "SELECT 1 FROM users WHERE student_no = 'S1101001'")
                && exists(connection, "SELECT 1 FROM books WHERE title = 'Clean Code'");
    }

    private static void clearLibrarySeedData(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM role_change_requests");
            statement.executeUpdate("DELETE FROM reservations");
            statement.executeUpdate("DELETE FROM favorites");
            statement.executeUpdate("DELETE FROM reviews");
            statement.executeUpdate("DELETE FROM borrow_records");
            statement.executeUpdate("DELETE FROM book_isbns");
            statement.executeUpdate("DELETE FROM books");
            statement.executeUpdate("DELETE FROM users");
            statement.executeUpdate("""
                    DELETE FROM sqlite_sequence
                    WHERE name IN ('users', 'books', 'book_isbns', 'borrow_records', 'reviews', 'favorites', 'reservations', 'role_change_requests')
                    """);
        }
    }

    private static void importInitialJsonData(Connection connection) throws IOException, SQLException {
        List<Map<String, Object>> users = readObjectArray(USERS_FILE);
        List<Map<String, Object>> books = readObjectArray(BOOKS_FILE);
        List<Map<String, Object>> borrowRecords = readObjectArray(BORROW_RECORDS_FILE);

        for (Map<String, Object> userData : users) {
            insertImportedUser(connection, userData);
        }

        for (Map<String, Object> bookData : books) {
            insertImportedBook(connection, bookData);
        }

        LocalDateTime importBaseTime = LocalDateTime.now();
        for (Map<String, Object> recordData : borrowRecords) {
            insertImportedBorrowRecord(connection, recordData, importBaseTime, users.size(), books.size());
        }

        System.out.printf(
                Locale.ROOT,
                "已從 JSON 匯入初始資料：%d 位使用者、%d 本書籍、%d 筆借閱紀錄%n",
                users.size(),
                books.size(),
                borrowRecords.size()
        );
    }

    private static List<Map<String, Object>> readObjectArray(String fileName) throws IOException {
        String jsonText = readSeedFile(fileName);
        Object parsed = SimpleJsonParser.parse(jsonText);
        if (!(parsed instanceof List<?> rawList)) {
            throw new IllegalArgumentException(fileName + " 的根節點必須是 JSON array。");
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : rawList) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                throw new IllegalArgumentException(fileName + " 陣列中的每個元素都必須是 JSON object。");
            }

            java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                map.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            result.add(map);
        }
        return result;
    }

    private static String readSeedFile(String fileName) throws IOException {
        List<Path> candidates = List.of(
                Path.of("data", fileName),
                Path.of(fileName),
                Path.of("src", "main", "resources", "seed", fileName)
        );

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return Files.readString(candidate, StandardCharsets.UTF_8);
            }
        }

        try (InputStream inputStream = Database.class.getResourceAsStream("/seed/" + fileName)) {
            if (inputStream != null) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        throw new IOException("找不到初始資料檔案：" + fileName + "。請確認 data/ 目錄存在。");
    }

    private static void insertImportedUser(Connection connection, Map<String, Object> data) throws SQLException {
        String sql = "INSERT INTO users(student_no, name, password, role_level, created_at, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, requiredString(data, "student_no"));
            ps.setString(2, requiredString(data, "name"));
            ps.setString(3, normalizeImportedPassword(requiredString(data, "password")));
            ps.setString(4, RolePolicy.normalize(requiredString(data, "role_level")));
            ps.setString(5, requiredString(data, "created_at"));
            ps.setString(6, requiredString(data, "status").toUpperCase(Locale.ROOT));
            ps.executeUpdate();
        }
    }

    private static void insertImportedBook(Connection connection, Map<String, Object> data) throws SQLException {
        String bookSql = """
                INSERT INTO books(title, authors, subjects, publisher, publish_year, edition, format_desc, source, note, active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """;

        try (PreparedStatement bookPs = connection.prepareStatement(bookSql, Statement.RETURN_GENERATED_KEYS)) {
            bookPs.setString(1, requiredString(data, "題名"));
            bookPs.setString(2, String.join(", ", stringList(data, "作者")));
            bookPs.setString(3, String.join(", ", stringList(data, "主題")));
            bookPs.setString(4, nullableString(data, "出版者"));
            bookPs.setString(5, nullableString(data, "出版年"));
            bookPs.setString(6, nullableString(data, "版本"));
            bookPs.setString(7, nullableString(data, "格式"));
            bookPs.setString(8, nullableString(data, "資料來源"));
            bookPs.setString(9, nullableString(data, "附註"));
            bookPs.executeUpdate();

            int bookId;
            try (ResultSet keys = bookPs.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("新增書籍失敗，無法取得 book_id");
                }
                bookId = keys.getInt(1);
            }

            List<String> isbns = stringList(data, "識別號");
            if (!isbns.isEmpty()) {
                try (PreparedStatement isbnPs = connection.prepareStatement("INSERT INTO book_isbns(book_id, isbn) VALUES (?, ?)")) {
                    for (String isbn : isbns) {
                        isbnPs.setInt(1, bookId);
                        isbnPs.setString(2, isbn);
                        isbnPs.addBatch();
                    }
                    isbnPs.executeBatch();
                }
            }
        }
    }

    private static void insertImportedBorrowRecord(Connection connection,
                                                   Map<String, Object> data,
                                                   LocalDateTime importBaseTime,
                                                   int userCount,
                                                   int bookCount) throws SQLException {
        int userId = requiredInt(data, "user_id");
        int bookId = requiredInt(data, "book_id");

        if (userId < 1 || userId > userCount) {
            throw new IllegalArgumentException("Borrow_records.json 的 user_id 超出範圍：" + userId);
        }
        if (bookId < 1 || bookId > bookCount) {
            throw new IllegalArgumentException("Borrow_records.json 的 book_id 超出範圍：" + bookId);
        }

        LocalDateTime borrowDate = resolveDateTime(requiredString(data, "borrow_date"), importBaseTime);
        LocalDateTime dueDate = resolveDateTime(requiredString(data, "due_date"), importBaseTime);
        LocalDateTime returnDate = resolveNullableDateTime(nullableString(data, "return_date"), importBaseTime);
        LocalDateTime createdAt = resolveDateTime(requiredString(data, "created_at"), importBaseTime);
        int borrowDays = requiredInt(data, "borrow_days");

        String sql = """
                INSERT INTO borrow_records(user_id, book_id, borrow_date, due_date, return_date, borrow_days, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, bookId);
            ps.setString(3, DateUtil.format(borrowDate));
            ps.setString(4, DateUtil.format(dueDate));
            if (returnDate == null) {
                ps.setNull(5, Types.VARCHAR);
            } else {
                ps.setString(5, DateUtil.format(returnDate));
            }
            ps.setInt(6, borrowDays);
            ps.setString(7, DateUtil.format(createdAt));
            ps.executeUpdate();
        }
    }

    private static int count(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static boolean exists(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return rs.next();
        }
    }

    private static void insertAdmin(Connection connection, String username, String rawPassword) throws SQLException {
        String sql = "INSERT INTO admins(username, password, created_at) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, SecurityUtil.sha256(rawPassword));
            ps.setString(3, DateUtil.nowString());
            ps.executeUpdate();
        }
    }

    private static String normalizeImportedPassword(String rawOrHashedPassword) {
        if (rawOrHashedPassword.matches("(?i)^[0-9a-f]{64}$")) {
            return rawOrHashedPassword.toLowerCase(Locale.ROOT);
        }
        return SecurityUtil.sha256(rawOrHashedPassword);
    }

    private static String requiredString(Map<String, Object> data, String key) {
        String value = nullableString(data, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("缺少必要欄位：" + key);
        }
        return value;
    }

    private static String nullableString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static int requiredInt(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            throw new IllegalArgumentException("缺少必要數值欄位：" + key);
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value).trim());
    }

    private static List<String> stringList(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> rawList) {
            List<String> result = new ArrayList<>();
            for (Object item : rawList) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }
        return List.of(String.valueOf(value));
    }

    private static LocalDateTime resolveNullableDateTime(String rawValue, LocalDateTime baseTime) {
        if (rawValue == null || rawValue.isBlank() || "null".equalsIgnoreCase(rawValue.trim())) {
            return null;
        }
        return resolveDateTime(rawValue, baseTime);
    }

    private static LocalDateTime resolveDateTime(String rawValue, LocalDateTime baseTime) {
        String value = rawValue.trim();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?i)^([+-]?\\d+)\\s+(day|days|hour|hours|minute|minutes)$")
                .matcher(value);
        if (matcher.matches()) {
            long amount = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toLowerCase(Locale.ROOT);
            return switch (unit) {
                case "day", "days" -> baseTime.plusDays(amount);
                case "hour", "hours" -> baseTime.plusHours(amount);
                case "minute", "minutes" -> baseTime.plusMinutes(amount);
                default -> throw new IllegalArgumentException("不支援的相對時間格式：" + rawValue);
            };
        }
        return DateUtil.parse(value);
    }
}
