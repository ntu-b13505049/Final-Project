package com.gymapp.model;

import java.time.LocalDateTime;

public class WorkoutLog {
    private int logId;
    private int memberId;
    private String exerciseName;
    private float weight;
    private int reps;
    private LocalDateTime workoutTime;

    public WorkoutLog() {}

    public WorkoutLog(int logId, int memberId, String exerciseName, float weight, int reps, LocalDateTime workoutTime) {
        this.logId = logId;
        this.memberId = memberId;
        this.exerciseName = exerciseName;
        this.weight = weight;
        this.reps = reps;
        this.workoutTime = workoutTime;
    }

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }
    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }
    public float getWeight() { return weight; }
    public void setWeight(float weight) { this.weight = weight; }
    public int getReps() { return reps; }
    public void setReps(int reps) { this.reps = reps; }
    public LocalDateTime getWorkoutTime() { return workoutTime; }
    public void setWorkoutTime(LocalDateTime workoutTime) { this.workoutTime = workoutTime; }
}
