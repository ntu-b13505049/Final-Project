package librarysystem.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class DateUtil {
    public static final DateTimeFormatter DB_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private DateUtil() {
    }

    public static String nowString() {
        return format(LocalDateTime.now());
    }

    public static String format(LocalDateTime value) {
        return value == null ? "" : value.format(DB_FORMATTER);
    }

    public static String formatDisplay(LocalDateTime value) {
        return value == null ? "未歸還" : value.format(DISPLAY_FORMATTER);
    }

    public static LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value, DB_FORMATTER);
    }

    public static long daysUntil(LocalDateTime target) {
        return ChronoUnit.DAYS.between(LocalDateTime.now().toLocalDate(), target.toLocalDate());
    }

    public static long overdueDays(LocalDateTime dueDate, LocalDateTime returnDate) {
        LocalDateTime compare = returnDate == null ? LocalDateTime.now() : returnDate;
        long days = ChronoUnit.DAYS.between(dueDate.toLocalDate(), compare.toLocalDate());
        return Math.max(days, 0);
    }
}
