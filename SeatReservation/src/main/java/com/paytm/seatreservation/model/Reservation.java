package com.paytm.seatreservation.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    private String id;

    @Column(name = "show_id", nullable = false)
    private String showId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(nullable = false)
    private String status; // CONFIRMED, CANCELLED

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Reservation() {}

    public Reservation(String id, String showId, String userId, Long amountPaise, String status) {
        this.id = id;
        this.showId = showId;
        this.userId = userId;
        this.amountPaise = amountPaise;
        this.status = status;
    }

    public String getId() { return id; }
    public String getShowId() { return showId; }
    public String getUserId() { return userId; }
    public Long getAmountPaise() { return amountPaise; }
    public String getStatus() { return status; }
}
