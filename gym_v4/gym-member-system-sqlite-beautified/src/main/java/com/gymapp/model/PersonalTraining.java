package com.gymapp.model;

import java.time.LocalDateTime;

public class PersonalTraining extends GymClass {
    public PersonalTraining(int courseId, String courseName, Integer trainerId, Integer branchId,
                            LocalDateTime scheduleTime, int maxCapacity, int enrolledCount, int pointsRequired) {
        super(courseId, courseName, "一對一", trainerId, branchId, scheduleTime, Math.max(1, maxCapacity), enrolledCount, pointsRequired);
    }

    @Override
    public boolean canReserve() {
        return getEnrolledCount() == 0;
    }
}
