package com.gymapp.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (IOException e) {
            System.err.println("無法讀取 db.properties: " + e.getMessage());
        }
    }

    private AppConfig() {}

    public static String get(String key, String defaultValue) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) {
            return sys;
        }
        String envKey = key.toUpperCase().replace('.', '_');
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return PROPS.getProperty(key, defaultValue);
    }

    /**
     * SQLite JDBC URL. 預設會在專案資料夾建立 data/gym_member_system.db。
     * 也可用 -Ddb.url=jdbc:sqlite:C:/path/gym_member_system.db 覆蓋。
     */
    public static String jdbcUrl() {
        return get("db.url", "jdbc:sqlite:data/gym_member_system.db");
    }

    /**
     * 保留舊介面，讓既有程式不需調整；SQLite 不需要另外連到 server。
     */
    public static String serverJdbcUrl() {
        return jdbcUrl();
    }

    /**
     * SQLite 不需要帳號密碼；保留給舊程式碼相容。
     */
    public static String dbUser() {
        return get("db.username", get("db.user", ""));
    }

    /**
     * SQLite 不需要帳號密碼；保留給舊程式碼相容。
     */
    public static String dbPassword() {
        return get("db.password", "");
    }
}
