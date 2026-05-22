package com.gymapp.model;

import java.time.LocalDateTime;

public class FitnessRecord {
    private int recordId;
    private int memberId;
    private Integer trainerId;
    private float weightKg;
    private float bodyFat;
    private float muscleMass;
    private String trainingContent;
    private String suggestion;
    private LocalDateTime recordedAt;

    public FitnessRecord() {}

    public FitnessRecord(int recordId, int memberId, Integer trainerId, float weightKg, float bodyFat, float muscleMass,
                         String trainingContent, String suggestion, LocalDateTime recordedAt) {
        this.recordId = recordId;
        this.memberId = memberId;
        this.trainerId = trainerId;
        this.weightKg = weightKg;
        this.bodyFat = bodyFat;
        this.muscleMass = muscleMass;
        this.trainingContent = trainingContent;
        this.suggestion = suggestion;
        this.recordedAt = recordedAt;
    }

    public int getRecordId() { return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }
    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public Integer getTrainerId() { return trainerId; }
    public void setTrainerId(Integer trainerId) { this.trainerId = trainerId; }
    public float getWeightKg() { return weightKg; }
    public void setWeightKg(float weightKg) { this.weightKg = weightKg; }
    public float getBodyFat() { return bodyFat; }
    public void setBodyFat(float bodyFat) { this.bodyFat = bodyFat; }
    public float getMuscleMass() { return muscleMass; }
    public void setMuscleMass(float muscleMass) { this.muscleMass = muscleMass; }
    public String getTrainingContent() { return trainingContent; }
    public void setTrainingContent(String trainingContent) { this.trainingContent = trainingContent; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
