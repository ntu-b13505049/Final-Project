package com.gymapp.observer;

import com.gymapp.model.GymClass;

import java.util.ArrayList;
import java.util.List;

public class CourseSeatSubject {
    private final List<ReservationObserver> observers = new ArrayList<>();

    public void addObserver(ReservationObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(ReservationObserver observer) {
        observers.remove(observer);
    }

    public void notifySeatAvailable(GymClass gymClass) {
        for (ReservationObserver observer : new ArrayList<>(observers)) {
            observer.onSeatAvailable(gymClass);
        }
    }
}
