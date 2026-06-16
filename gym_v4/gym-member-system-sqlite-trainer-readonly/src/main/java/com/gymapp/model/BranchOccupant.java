package com.gymapp.model;

import java.time.LocalDateTime;

public class BranchOccupant {
    private final int branchId;
    private final String branchName;
    private final int memberId;
    private final String memberName;
    private final String account;
    private final String status;
    private final LocalDateTime enteredAt;

    public BranchOccupant(int branchId, String branchName, int memberId, String memberName, String account, String status, LocalDateTime enteredAt) {
        this.branchId = branchId;
        this.branchName = branchName;
        this.memberId = memberId;
        this.memberName = memberName;
        this.account = account;
        this.status = status;
        this.enteredAt = enteredAt;
    }

    public int getBranchId() { return branchId; }
    public String getBranchName() { return branchName; }
    public int getMemberId() { return memberId; }
    public String getMemberName() { return memberName; }
    public String getAccount() { return account; }
    public String getStatus() { return status; }
    public LocalDateTime getEnteredAt() { return enteredAt; }
}
