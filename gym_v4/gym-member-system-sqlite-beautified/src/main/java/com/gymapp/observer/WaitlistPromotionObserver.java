package com.gymapp.observer;

import com.gymapp.model.GymClass;
import com.gymapp.service.ReservationService;

public class WaitlistPromotionObserver implements ReservationObserver {
    private final ReservationService reservationService;

    public WaitlistPromotionObserver(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Override
    public void onSeatAvailable(GymClass gymClass) {
        reservationService.promoteNextWaitlistedMember(gymClass.getCourseId());
    }
}
