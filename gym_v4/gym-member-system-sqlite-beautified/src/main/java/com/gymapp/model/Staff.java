package com.gymapp.model;

public class Staff extends User {
    private String phone;
    private String specialty;
    private Integer branchId;

    public Staff() {}

    public Staff(int id, String name, String account, String password, Role role, String phone, String specialty, Integer branchId) {
        super(id, name, account, password, role);
        this.phone = phone;
        this.specialty = specialty;
        this.branchId = branchId;
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }
}
