package com.paytm.seatreservation.dto;

import java.util.Map;

public class ShowStateResponse {
    private String show_id;
    private int total_seats;
    private int available_count;
    private int confirmed_count;
    private Map<String, String> seats_status;

    public ShowStateResponse(String show_id, int total_seats, int available_count, int confirmed_count, Map<String, String> seats_status) {
        this.show_id = show_id;
        this.total_seats = total_seats;
        this.available_count = available_count;
        this.confirmed_count = confirmed_count;
        this.seats_status = seats_status;
    }

    public String getShow_id() { return show_id; }
    public int getTotal_seats() { return total_seats; }
    public int getAvailable_count() { return available_count; }
    public int getConfirmed_count() { return confirmed_count; }
    public Map<String, String> getSeats_status() { return seats_status; }
}
