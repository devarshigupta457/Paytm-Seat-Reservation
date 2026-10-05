package com.paytm.seatreservation.model;

import jakarta.persistence.*;

@Entity
@Table(name = "seats", uniqueConstraints = {
        @UniqueConstraint(name = "uk_show_seat", columnNames = {"show_id", "seat_number"})
})
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "show_id", nullable = false)
    private String showId;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @Column(nullable = false)
    private String status = "AVAILABLE"; // AVAILABLE, CONFIRMED

    @Column(name = "reservation_id")
    private String reservationId;

    @Version
    private Long version;

    public Seat() {}

    public Seat(String showId, String seatNumber) {
        this.showId = showId;
        this.seatNumber = seatNumber;
        this.status = "AVAILABLE";
    }

    public Long getId() { return id; }
    public String getShowId() { return showId; }
    public String getSeatNumber() { return seatNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
}
