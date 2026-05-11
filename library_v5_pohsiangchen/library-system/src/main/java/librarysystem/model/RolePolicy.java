package librarysystem.model;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class RolePolicy {
    public static final String NORMAL = "NORMAL";
    public static final String VIP = "VIP";
    public static final String GOLD = "GOLD";
    public static final String PLATINUM = "PLATINUM";

    private static final List<String> SUPPORTED_LEVELS = List.of(NORMAL, VIP, GOLD, PLATINUM);

    private RolePolicy() {
    }

    public static List<String> supportedLevels() {
        return SUPPORTED_LEVELS;
    }

    public static String normalize(String roleLevel) {
        if (roleLevel == null || roleLevel.isBlank()) {
            return NORMAL;
        }
        String value = roleLevel.trim().toUpperCase(Locale.ROOT);
        return SUPPORTED_LEVELS.contains(value) ? value : NORMAL;
    }

    public static boolean isSupported(String roleLevel) {
        return roleLevel != null && SUPPORTED_LEVELS.contains(roleLevel.trim().toUpperCase(Locale.ROOT));
    }

    public static boolean canUpgradeTo(String targetLevel) {
        String normalized = normalize(targetLevel);
        return !NORMAL.equals(normalized);
    }

    public static int borrowLimit(String roleLevel) {
        return switch (normalize(roleLevel)) {
            case PLATINUM -> 10;
            case GOLD -> 8;
            case VIP -> 5;
            default -> 3;
        };
    }

    public static int reservationLimit(String roleLevel) {
        return switch (normalize(roleLevel)) {
            case PLATINUM -> 10;
            case GOLD -> 8;
            case VIP -> 5;
            default -> 3;
        };
    }

    public static int[] allowedDurations(String roleLevel) {
        return switch (normalize(roleLevel)) {
            case PLATINUM -> new int[]{1, 3, 7, 14, 21, 30};
            case GOLD -> new int[]{1, 3, 7, 14, 21};
            case VIP -> new int[]{1, 3, 7, 14};
            default -> new int[]{1, 3, 7};
        };
    }

    public static int finePerDay(String roleLevel) {
        return switch (normalize(roleLevel)) {
            case PLATINUM -> 1;
            case GOLD -> 2;
            case VIP -> 3;
            default -> 5;
        };
    }

    public static boolean canUseFavorites(String roleLevel) {
        return !NORMAL.equals(normalize(roleLevel));
    }

    public static String displayName(String roleLevel) {
        return switch (normalize(roleLevel)) {
            case PLATINUM -> "PLATINUM 白金";
            case GOLD -> "GOLD 金級";
            case VIP -> "VIP";
            default -> "NORMAL 普通";
        };
    }

    public static String durationText(String roleLevel) {
        int[] days = allowedDurations(roleLevel);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < days.length; i++) {
            if (i > 0) {
                sb.append(" / ");
            }
            sb.append(days[i]).append("天");
        }
        return sb.toString();
    }

    public static String description(String roleLevel) {
        String normalized = normalize(roleLevel);
        return String.format(
                "%s：同時最多 %d 本、可選期限 %s、預約上限 %d 本、逾期罰款 %d 元/天、收藏功能：%s",
                displayName(normalized),
                borrowLimit(normalized),
                durationText(normalized),
                reservationLimit(normalized),
                finePerDay(normalized),
                canUseFavorites(normalized) ? "可用" : "不可用"
        );
    }

    public static String supportedLevelsText() {
        return String.join(", ", SUPPORTED_LEVELS);
    }

    public static String[] upgradeTargets() {
        return SUPPORTED_LEVELS.stream().filter(level -> !NORMAL.equals(level)).toArray(String[]::new);
    }

    public static String[] allLevelsArray() {
        return SUPPORTED_LEVELS.toArray(new String[0]);
    }

    public static boolean durationAllowed(String roleLevel, int days) {
        return Arrays.stream(allowedDurations(roleLevel)).anyMatch(value -> value == days);
    }
}
