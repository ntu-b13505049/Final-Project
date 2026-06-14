package com.gymapp.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FollowUpRecord {
    private int followId;
    private int memberId;
    private Integer trainerId;
    private String goal;
    private String currentStatus;
    private LocalDate nextFollowDate;
    private String suggestion;
    private LocalDateTime createdAt;

    public FollowUpRecord() {}

    public FollowUpRecord(int followId, int memberId, Integer trainerId, String goal, String currentStatus,
                          LocalDate nextFollowDate, String suggestion, LocalDateTime createdAt) {
        this.followId = followId;
        this.memberId = memberId;
        this.trainerId = trainerId;
        this.goal = goal;
        this.currentStatus = currentStatus;
        this.nextFollowDate = nextFollowDate;
        this.suggestion = suggestion;
        this.createdAt = createdAt;
    }

    public int getFollowId() { return followId; }
    public void setFollowId(int followId) { this.followId = followId; }
    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public Integer getTrainerId() { return trainerId; }
    public void setTrainerId(Integer trainerId) { this.trainerId = trainerId; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
    public LocalDate getNextFollowDate() { return nextFollowDate; }
    public void setNextFollowDate(LocalDate nextFollowDate) { this.nextFollowDate = nextFollowDate; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
