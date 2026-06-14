package com.gymapp.model;

public class Trainer extends Staff {
    public Trainer(int id, String name, String account, String password, String phone, String specialty, Integer branchId) {
        super(id, name, account, password, Role.TRAINER, phone, specialty, branchId);
    }
}
