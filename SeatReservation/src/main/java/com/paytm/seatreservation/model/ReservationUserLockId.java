package com.paytm.seatreservation.model;

import java.io.Serializable;
import java.util.Objects;

public class ReservationUserLockId implements Serializable {

    private String showId;
    private String userId;

    public ReservationUserLockId() {
    }

    public ReservationUserLockId(String showId, String userId) {
        this.showId = showId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof ReservationUserLockId)) {
            return false;
        }

        ReservationUserLockId that =
                (ReservationUserLockId) o;

        return Objects.equals(showId, that.showId)
                && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(showId, userId);
    }
}