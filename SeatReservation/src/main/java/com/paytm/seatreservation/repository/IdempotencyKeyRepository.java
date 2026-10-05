package com.paytm.seatreservation.repository;

import com.paytm.seatreservation.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
    Optional<IdempotencyKey> findByIdempotencyKeyAndUserId(String idempotencyKey, String userId);
}