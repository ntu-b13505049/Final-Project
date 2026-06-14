package com.gymapp.observer;

import com.gymapp.model.GymClass;

public interface ReservationObserver {
    void onSeatAvailable(GymClass gymClass);
}
