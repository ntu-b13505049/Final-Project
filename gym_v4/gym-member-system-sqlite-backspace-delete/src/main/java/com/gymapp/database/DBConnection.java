package com.gymapp.database;

import com.gymapp.config.AppConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DBConnection {
    private static final String SQLITE_PREFIX = "jdbc:sqlite:";

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        loadDriverIfPresent();
        ensureSqliteDirectoryExists(AppConfig.jdbcUrl());
        Connection con = DriverManager.getConnection(AppConfig.jdbcUrl());
        configureSqliteConnection(con);
        return con;
    }

    public static Connection getServerConnection() throws SQLException {
        return getConnection();
    }

    private static void loadDriverIfPresent() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {
            // 若執行時未放 sqlite-jdbc，DriverManager 會拋出 No suitable driver 的明確錯誤。
        }
    }

    private static void configureSqliteConnection(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA busy_timeout = 5000");
        }
    }

    private static void ensureSqliteDirectoryExists(String jdbcUrl) throws SQLException {
        if (jdbcUrl == null || !jdbcUrl.startsWith(SQLITE_PREFIX)) {
            return;
        }
        String pathText = jdbcUrl.substring(SQLITE_PREFIX.length()).trim();
        if (pathText.isEmpty() || ":memory:".equals(pathText) || pathText.startsWith("file:")) {
            return;
        }
        try {
            Path dbPath = Path.of(pathText);
            Path parent = dbPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception e) {
            throw new SQLException("無法建立 SQLite 資料庫資料夾：" + pathText, e);
        }
    }
}
