package com.gymapp.model;

import java.time.LocalDateTime;

public class WaitlistEntry {
    private int waitlistId;
    private int courseId;
    private int memberId;
    private String status;
    private LocalDateTime createdTime;

    public WaitlistEntry() {}

    public WaitlistEntry(int waitlistId, int courseId, int memberId, String status, LocalDateTime createdTime) {
        this.waitlistId = waitlistId;
        this.courseId = courseId;
        this.memberId = memberId;
        this.status = status;
        this.createdTime = createdTime;
    }

    public int getWaitlistId() { return waitlistId; }
    public void setWaitlistId(int waitlistId) { this.waitlistId = waitlistId; }
    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }
    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
}
