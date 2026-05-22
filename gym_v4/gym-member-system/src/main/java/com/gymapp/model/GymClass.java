package com.gymapp.model;

import java.time.LocalDateTime;

public abstract class GymClass {
    private int courseId;
    private String courseName;
    private String courseType;
    private Integer trainerId;
    private Integer branchId;
    private LocalDateTime scheduleTime;
    private int maxCapacity;
    private int enrolledCount;
    private int pointsRequired;

    protected GymClass() {}

    protected GymClass(int courseId, String courseName, String courseType, Integer trainerId, Integer branchId,
                       LocalDateTime scheduleTime, int maxCapacity, int enrolledCount, int pointsRequired) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseType = courseType;
        this.trainerId = trainerId;
        this.branchId = branchId;
        this.scheduleTime = scheduleTime;
        this.maxCapacity = maxCapacity;
        this.enrolledCount = enrolledCount;
        this.pointsRequired = pointsRequired;
    }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getCourseType() { return courseType; }
    public void setCourseType(String courseType) { this.courseType = courseType; }
    public Integer getTrainerId() { return trainerId; }
    public void setTrainerId(Integer trainerId) { this.trainerId = trainerId; }
    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }
    public LocalDateTime getScheduleTime() { return scheduleTime; }
    public void setScheduleTime(LocalDateTime scheduleTime) { this.scheduleTime = scheduleTime; }
    public int getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }
    public int getEnrolledCount() { return enrolledCount; }
    public void setEnrolledCount(int enrolledCount) { this.enrolledCount = enrolledCount; }
    public int getPointsRequired() { return pointsRequired; }
    public void setPointsRequired(int pointsRequired) { this.pointsRequired = pointsRequired; }

    public int getRemainingSpots() {
        return Math.max(maxCapacity - enrolledCount, 0);
    }

    public abstract boolean canReserve();
}
