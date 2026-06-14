package com.gymapp.model;

public abstract class User {
    private int id;
    private String name;
    private String account;
    private String password;
    private Role role;

    protected User() {}

    protected User(int id, String name, String account, String password, Role role) {
        this.id = id;
        this.name = name;
        this.account = account;
        this.password = password;
        this.role = role;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getDisplayRole() {
        return role == null ? "" : role.displayName();
    }

    public boolean hasRole(Role expected) {
        return role == expected;
    }
}
