package com.gymapp.util;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateTimeUtil {
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DB_DATE_TIME_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeUtil() {}

    public static String format(LocalDateTime time) {
        return time == null ? "" : DATE_TIME.format(time);
    }

    public static String format(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    public static LocalDateTime parseDateTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.trim(), DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期時間格式需為 yyyy-MM-dd HH:mm，例如 2026-03-10 18:30");
        }
    }

    public static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期格式需為 yyyy-MM-dd，例如 2026-03-10");
        }
    }

    public static Timestamp toTimestamp(LocalDateTime time) {
        return time == null ? null : Timestamp.valueOf(time);
    }

    public static Date toSqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }

    public static LocalDateTime fromTimestamp(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    public static LocalDate fromSqlDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    public static LocalDateTime fromDbTimestamp(ResultSet rs, String column) throws SQLException {
        String text = rs.getString(column);
        return parseDbDateTime(text);
    }

    public static LocalDate fromDbDate(ResultSet rs, String column) throws SQLException {
        String text = rs.getString(column);
        if (text == null || text.isBlank()) {
            return null;
        }
        String cleaned = text.trim();
        if (cleaned.length() >= 10) {
            cleaned = cleaned.substring(0, 10);
        }
        try {
            return LocalDate.parse(cleaned, DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static LocalDateTime parseDbDateTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String cleaned = text.trim().replace('T', ' ');
        int dot = cleaned.indexOf('.');
        if (dot >= 0) {
            cleaned = cleaned.substring(0, dot);
        }
        try {
            if (cleaned.length() >= 19) {
                return LocalDateTime.parse(cleaned.substring(0, 19), DB_DATE_TIME_SECONDS);
            }
            if (cleaned.length() >= 16) {
                return LocalDateTime.parse(cleaned.substring(0, 16), DATE_TIME);
            }
            if (cleaned.length() == 10) {
                return LocalDate.parse(cleaned, DATE).atStartOfDay();
            }
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }
}
