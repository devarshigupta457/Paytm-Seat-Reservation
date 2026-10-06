package com.paytm.seatreservation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "reservation_user_locks")
@IdClass(ReservationUserLockId.class)
public class ReservationUserLock {

    @Id
    private String showId;

    @Id
    private String userId;

    public ReservationUserLock() {
    }

    public ReservationUserLock(String showId, String userId) {
        this.showId = showId;
        this.userId = userId;
    }

    public String getShowId() {
        return showId;
    }

    public void setShowId(String showId) {
        this.showId = showId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}