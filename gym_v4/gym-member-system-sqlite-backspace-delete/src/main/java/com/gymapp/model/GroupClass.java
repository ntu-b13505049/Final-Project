package com.gymapp.model;

import java.time.LocalDateTime;

public class GroupClass extends GymClass {
    public GroupClass(int courseId, String courseName, Integer trainerId, Integer branchId,
                      LocalDateTime scheduleTime, int maxCapacity, int enrolledCount, int pointsRequired) {
        super(courseId, courseName, "團課", trainerId, branchId, scheduleTime, maxCapacity, enrolledCount, pointsRequired);
    }

    @Override
    public boolean canReserve() {
        return getRemainingSpots() > 0;
    }
}
