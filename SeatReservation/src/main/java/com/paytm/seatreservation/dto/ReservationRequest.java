package com.paytm.seatreservation.dto;

import java.util.List;

public class ReservationRequest {

    private String name; // Add this field
    private List<String> seats;
    private Long price_paise;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSeats() {
        return seats;
    }

    public void setSeats(List<String> seats) {
        this.seats = seats;
    }

    public Long getPrice_paise() {
        return price_paise;
    }

    public void setPrice_paise(Long price_paise) {
        this.price_paise = price_paise;
    }
}