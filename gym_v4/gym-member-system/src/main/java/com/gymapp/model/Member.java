package com.gymapp.model;

public class Member extends User {
    private String phone;
    private String email;
    private String status;
    private Wallet wallet;

    public Member() {
        setRole(Role.MEMBER);
        this.wallet = new Wallet(0);
        this.status = "Active";
    }

    public Member(int id, String name, String account, String password, String phone, String email, String status, int walletBalance) {
        super(id, name, account, password, Role.MEMBER);
        this.phone = phone;
        this.email = email;
        this.status = status;
        this.wallet = new Wallet(Math.max(walletBalance, 0));
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Wallet getWallet() { return wallet; }
    public void setWallet(Wallet wallet) { this.wallet = wallet; }

    public boolean isActive() {
        return "Active".equalsIgnoreCase(status) || "啟用".equals(status);
    }
}
