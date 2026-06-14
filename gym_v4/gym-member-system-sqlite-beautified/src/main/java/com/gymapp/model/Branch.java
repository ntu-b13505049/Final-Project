package com.gymapp.model;

public class Branch {
    private int branchId;
    private String branchName;
    private int maxCapacity;
    private int currentCapacity;

    public Branch() {}

    public Branch(int branchId, String branchName, int maxCapacity, int currentCapacity) {
        this.branchId = branchId;
        this.branchName = branchName;
        this.maxCapacity = maxCapacity;
        this.currentCapacity = currentCapacity;
    }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public int getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }
    public int getCurrentCapacity() { return currentCapacity; }
    public void setCurrentCapacity(int currentCapacity) { this.currentCapacity = currentCapacity; }

    public int getRemainingCapacity() {
        return Math.max(maxCapacity - currentCapacity, 0);
    }
}
