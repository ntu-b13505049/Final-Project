package com.gymapp.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {
    private DatabaseInitializer() {}

    public static void ensureSchema() throws SQLException, IOException {
        String sql = readSchema();
        try (Connection con = DBConnection.getConnection()) {
            executeScript(con, sql);
        }
    }

    private static String readSchema() throws IOException {
        InputStream in = DatabaseInitializer.class.getClassLoader().getResourceAsStream("schema.sql");
        if (in == null) {
            in = DatabaseInitializer.class.getClassLoader().getResourceAsStream("sql/schema.sql");
        }
        if (in == null) {
            java.nio.file.Path local = java.nio.file.Path.of("sql", "schema.sql");
            if (java.nio.file.Files.exists(local)) {
                return java.nio.file.Files.readString(local, StandardCharsets.UTF_8);
            }
            throw new IOException("找不到 schema.sql，請確認 sql/schema.sql 存在或已複製到 classpath。");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }

    private static void executeScript(Connection con, String script) throws SQLException {
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < script.length(); i++) {
            char ch = script.charAt(i);
            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
            if (ch == ';' && !inSingleQuote && !inDoubleQuote) {
                executeStatement(con, current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        executeStatement(con, current.toString());
    }

    private static void executeStatement(Connection con, String sql) throws SQLException {
        String cleaned = removeSqlComments(sql).trim();
        if (cleaned.isEmpty()) {
            return;
        }
        try (Statement st = con.createStatement()) {
            st.execute(cleaned);
        }
    }

    private static String removeSqlComments(String sql) {
        StringBuilder out = new StringBuilder();
        String[] lines = sql.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("--") && !trimmed.startsWith("#")) {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }
}
