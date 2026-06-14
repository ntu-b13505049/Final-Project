package com.gymapp.model;

public class RechargePlan {
    private int planId;
    private String planName;
    private int payAmount;
    private int points;
    private String description;

    public RechargePlan() {}

    public RechargePlan(int planId, String planName, int payAmount, int points, String description) {
        this.planId = planId;
        this.planName = planName;
        this.payAmount = payAmount;
        this.points = points;
        this.description = description;
    }

    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public int getPayAmount() { return payAmount; }
    public void setPayAmount(int payAmount) { this.payAmount = payAmount; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return planId + " - " + planName + "（付款 " + payAmount + " / 點數 " + points + "）";
    }
}
