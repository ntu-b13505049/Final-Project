package com.gymapp.model;

public class Admin extends Staff {
    public Admin(int id, String name, String account, String password, String phone, String specialty, Integer branchId) {
        super(id, name, account, password, Role.ADMIN, phone, specialty, branchId);
    }
}
