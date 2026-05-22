package com.gymapp.model;

public enum Role {
    ADMIN("管理員"),
    TRAINER("教練"),
    MEMBER("會員");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static Role fromDisplayName(String text) {
        for (Role role : values()) {
            if (role.displayName.equals(text) || role.name().equalsIgnoreCase(text)) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知角色：" + text);
    }
}
