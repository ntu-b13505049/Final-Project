package com.gymapp.model;

import java.time.LocalDateTime;

public final class GymClassFactory {
    private GymClassFactory() {}

    public static GymClass create(int courseId, String courseName, String courseType, Integer trainerId, Integer branchId,
                                  LocalDateTime scheduleTime, int maxCapacity, int enrolledCount, int pointsRequired) {
        if ("一對一".equals(courseType) || "Personal".equalsIgnoreCase(courseType)) {
            return new PersonalTraining(courseId, courseName, trainerId, branchId, scheduleTime, maxCapacity, enrolledCount, pointsRequired);
        }
        return new GroupClass(courseId, courseName, trainerId, branchId, scheduleTime, maxCapacity, enrolledCount, pointsRequired);
    }
}
