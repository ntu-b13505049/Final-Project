package com.gymapp.model;

import java.time.LocalDate;

public class Equipment {
    private int equipmentId;
    private String equipmentName;
    private String type;
    private String status;
    private LocalDate purchaseDate;
    private LocalDate lastMaintenanceDate;
    private LocalDate nextMaintenanceDate;
    private Integer branchId;
    private String notes;

    public Equipment() {}

    public Equipment(int equipmentId, String equipmentName, String type, String status, LocalDate purchaseDate,
                     LocalDate lastMaintenanceDate, LocalDate nextMaintenanceDate, Integer branchId, String notes) {
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.type = type;
        this.status = status;
        this.purchaseDate = purchaseDate;
        this.lastMaintenanceDate = lastMaintenanceDate;
        this.nextMaintenanceDate = nextMaintenanceDate;
        this.branchId = branchId;
        this.notes = notes;
    }

    public int getEquipmentId() { return equipmentId; }
    public void setEquipmentId(int equipmentId) { this.equipmentId = equipmentId; }
    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public LocalDate getLastMaintenanceDate() { return lastMaintenanceDate; }
    public void setLastMaintenanceDate(LocalDate lastMaintenanceDate) { this.lastMaintenanceDate = lastMaintenanceDate; }
    public LocalDate getNextMaintenanceDate() { return nextMaintenanceDate; }
    public void setNextMaintenanceDate(LocalDate nextMaintenanceDate) { this.nextMaintenanceDate = nextMaintenanceDate; }
    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
