package com.paytm.seatreservation.dto;

import java.util.List;

public class ShowResponse {
    private String id;
    private String name;
    private List<String> seats;
    private Long price_paise;

    public ShowResponse(String id, String name, List<String> seats, Long price_paise) {
        this.id = id;
        this.name = name;
        this.seats = seats;
        this.price_paise = price_paise;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<String> getSeats() { return seats; }
    public Long getPrice_paise() { return price_paise; }
}
