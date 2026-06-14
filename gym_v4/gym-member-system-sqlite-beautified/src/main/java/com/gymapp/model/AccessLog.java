package com.gymapp.model;

import java.time.LocalDateTime;

public class AccessLog {
    private int logId;
    private int memberId;
    private int branchId;
    private String action;
    private LocalDateTime timestamp;

    public AccessLog() {}

    public AccessLog(int logId, int memberId, int branchId, String action, LocalDateTime timestamp) {
        this.logId = logId;
        this.memberId = memberId;
        this.branchId = branchId;
        this.action = action;
        this.timestamp = timestamp;
    }

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }
    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
