package com.gymapp.model;

import java.time.LocalDateTime;

public class SaleRecord {
    private int saleId;
    private Integer memberId;
    private int productId;
    private int quantity;
    private int totalAmount;
    private Integer soldBy;
    private LocalDateTime timestamp;

    public SaleRecord() {}

    public SaleRecord(int saleId, Integer memberId, int productId, int quantity, int totalAmount, Integer soldBy, LocalDateTime timestamp) {
        this.saleId = saleId;
        this.memberId = memberId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.soldBy = soldBy;
        this.timestamp = timestamp;
    }

    public int getSaleId() { return saleId; }
    public void setSaleId(int saleId) { this.saleId = saleId; }
    public Integer getMemberId() { return memberId; }
    public void setMemberId(Integer memberId) { this.memberId = memberId; }
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getTotalAmount() { return totalAmount; }
    public void setTotalAmount(int totalAmount) { this.totalAmount = totalAmount; }
    public Integer getSoldBy() { return soldBy; }
    public void setSoldBy(Integer soldBy) { this.soldBy = soldBy; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
