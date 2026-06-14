package com.gymapp.model;

import java.time.LocalDateTime;

public class TransactionRecord {
    private int transactionId;
    private int memberId;
    private String type;
    private int amount;
    private LocalDateTime timestamp;
    private String note;

    public TransactionRecord() {}

    public TransactionRecord(int transactionId, int memberId, String type, int amount, LocalDateTime timestamp, String note) {
        this.transactionId = transactionId;
        this.memberId = memberId;
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
        this.note = note;
    }

    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }
    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
