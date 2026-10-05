package com.paytm.seatreservation.model;


import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {

    @Id
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "reservation_id", nullable = false)
    private String reservationId;

    @Column(name = "response_json", nullable = false, columnDefinition = "TEXT")
    private String responseJson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public IdempotencyKey() {}

    public IdempotencyKey(String idempotencyKey, String userId, String requestHash, String reservationId, String responseJson) {
        this.idempotencyKey = idempotencyKey;
        this.userId = userId;
        this.requestHash = requestHash;
        this.reservationId = reservationId;
        this.responseJson = responseJson;
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public String getUserId() { return userId; }
    public String getRequestHash() { return requestHash; }
    public String getReservationId() { return reservationId; }
    public String getResponseJson() { return responseJson; }
}
