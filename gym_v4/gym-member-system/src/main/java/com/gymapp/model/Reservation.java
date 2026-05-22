package com.gymapp.model;

import java.time.LocalDateTime;

public class Reservation {
    private int reservationId;
    private int memberId;
    private int courseId;
    private String status;
    private int pointsDeducted;
    private LocalDateTime createdTime;

    public Reservation() {}

    public Reservation(int reservationId, int memberId, int courseId, String status, int pointsDeducted, LocalDateTime createdTime) {
        this.reservationId = reservationId;
        this.memberId = memberId;
        this.courseId = courseId;
        this.status = status;
        this.pointsDeducted = pointsDeducted;
        this.createdTime = createdTime;
    }

    public int getReservationId() { return reservationId; }
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }
    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getPointsDeducted() { return pointsDeducted; }
    public void setPointsDeducted(int pointsDeducted) { this.pointsDeducted = pointsDeducted; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
}
