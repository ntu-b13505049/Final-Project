package com.gymapp.model;

public class Wallet {
    private int balance;

    public Wallet(int balance) {
        if (balance < 0) {
            throw new IllegalArgumentException("錢包餘額不可為負數");
        }
        this.balance = balance;
    }

    public int getBalance() {
        return balance;
    }

    public void deposit(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("儲值金額必須大於 0");
        }
        balance += amount;
    }

    public void deduct(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("扣點金額必須大於 0");
        }
        if (balance < amount) {
            throw new IllegalArgumentException("錢包餘額不足");
        }
        balance -= amount;
    }
}
