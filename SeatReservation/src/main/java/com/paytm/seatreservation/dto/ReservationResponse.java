package com.paytm.seatreservation.dto;

import java.util.List;

public class ReservationResponse {
    private String reservation_id;
    private String show_id;
    private String user_id;
    private List<String> seats;
    private Long amount_paise;
    private String status;

    public ReservationResponse() {}

    public ReservationResponse(String reservation_id, String show_id, String user_id, List<String> seats, Long amount_paise, String status) {
        this.reservation_id = reservation_id;
        this.show_id = show_id;
        this.user_id = user_id;
        this.seats = seats;
        this.amount_paise = amount_paise;
        this.status = status;
    }

    public String getReservation_id() { return reservation_id; }
    public String getShow_id() { return show_id; }
    public String getUser_id() { return user_id; }
    public List<String> getSeats() { return seats; }
    public Long getAmount_paise() { return amount_paise; }
    public String getStatus() { return status; }
}
