package com.paytm.seatreservation.dto;

import java.util.List;

public class ReservationRequest {
    private List<String> seats;

    public List<String> getSeats() { return seats; }
    public void setSeats(List<String> seats) { this.seats = seats; }
}
