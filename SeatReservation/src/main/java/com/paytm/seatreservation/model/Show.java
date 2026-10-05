package com.paytm.seatreservation.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shows")
public class Show {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_paise", nullable = false)
    private Long pricePaise;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Show() {}

    public Show(String id, String name, Long pricePaise) {
        this.id = id;
        this.name = name;
        this.pricePaise = pricePaise;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Long getPricePaise() { return pricePaise; }
}