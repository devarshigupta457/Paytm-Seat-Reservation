package com.paytm.seatreservation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paytm.seatreservation.dto.*;
import com.paytm.seatreservation.exception.DomainConflictException;
import com.paytm.seatreservation.exception.ForbiddenException;
import com.paytm.seatreservation.model.*;
import com.paytm.seatreservation.repository.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ReservationService {

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final IdempotencyKeyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    private final Counter confirmedCounter;
    private final Counter seatTakenCounter;
    private final Counter limitExceededCounter;
    private final Counter idempotencyMismatchCounter;

    public ReservationService(
            ShowRepository showRepository,
            SeatRepository seatRepository,
            ReservationRepository reservationRepository,
            IdempotencyKeyRepository idempotencyRepository,
            ObjectMapper objectMapper,
            MeterRegistry registry) {
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
        this.reservationRepository = reservationRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;

        this.confirmedCounter = registry.counter("reservations.confirmed");
        this.seatTakenCounter = registry.counter("reservations.declined", "reason", "seat_taken");
        this.limitExceededCounter = registry.counter("reservations.declined", "reason", "user_limit_exceeded");
        this.idempotencyMismatchCounter = registry.counter("reservations.declined", "reason", "idempotency_mismatch");
    }

    @Transactional
    public ReservationResponse cancelReservation(String reservationId, String userId) {

        // 1. Find reservation
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() ->
                        new DomainConflictException(
                                "Reservation not found: " + reservationId));

        // 2. Only the owner can cancel
        if (!reservation.getUserId().equals(userId)) {
            throw new ForbiddenException(
                    "Only the reservation owner may cancel this reservation");
        }

        // 3. Reservation must be CONFIRMED
        if (!"CONFIRMED".equals(reservation.getStatus())) {
            throw new DomainConflictException(
                    "Reservation is already " + reservation.getStatus());
        }

        // 4. Lock all seats belonging to this reservation
        List<Seat> lockedSeats =
                seatRepository.findByReservationIdForUpdate(reservationId);

        // 5. Make sure reservation still owns seats
        if (lockedSeats.isEmpty()) {
            throw new DomainConflictException(
                    "No seats are associated with reservation: "
                            + reservationId);
        }

        // 6. Safety check
        for (Seat seat : lockedSeats) {

            if (!reservationId.equals(seat.getReservationId())) {
                throw new DomainConflictException(
                        "Seat " + seat.getSeatNumber()
                                + " is no longer associated with reservation "
                                + reservationId);
            }

            if (!"CONFIRMED".equals(seat.getStatus())) {
                throw new DomainConflictException(
                        "Seat " + seat.getSeatNumber()
                                + " is not in CONFIRMED state");
            }
        }

        // 7. Release seats
        List<String> seatNumbers = new ArrayList<>();

        for (Seat seat : lockedSeats) {

            seatNumbers.add(seat.getSeatNumber());

            seat.setStatus("AVAILABLE");
            seat.setReservationId(null);
        }

        seatRepository.saveAll(lockedSeats);

        // 8. Mark reservation as CANCELLED
        reservation.setStatus("CANCELLED");

        reservationRepository.save(reservation);

        // 9. Return response
        return new ReservationResponse(
                reservation.getId(),
                reservation.getShowId(),
                reservation.getUserId(),
                seatNumbers,
                reservation.getAmountPaise(),
                "cancelled"
        );
    }

    @Transactional
    public ReservationResponse reserveSeats(String showId, String userId, String idempotencyKey, ReservationRequest request) {
        // 1. Idempotency Check
        String requestHash = DigestUtils.sha256Hex(request.getSeats().toString());
        Optional<IdempotencyKey> existingKey = idempotencyRepository.findByIdempotencyKeyAndUserId(idempotencyKey, userId);

        if (existingKey.isPresent()) {
            IdempotencyKey ik = existingKey.get();
            if (!ik.getRequestHash().equals(requestHash)) {
                idempotencyMismatchCounter.increment();
                throw new DomainConflictException("Idempotency key reused with different payload");
            }
            try {
                return objectMapper.readValue(ik.getResponseJson(), ReservationResponse.class);
            } catch (Exception e) {
                throw new RuntimeException("Error parsing cached response", e);
            }
        }

        // 2. Fetch Show
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new DomainConflictException("Show not found: " + showId));

        List<String> requestedSeats = request.getSeats();
        int perUserLimit = 4;

        // 3. User Limit Check
        long existingCount = seatRepository.countByShowIdAndReservationUserId(showId, userId);
        if (existingCount + requestedSeats.size() > perUserLimit) {
            limitExceededCounter.increment();
            throw new DomainConflictException("Exceeds maximum limit of " + perUserLimit + " seats per user");
        }

        // 4. Sort seat numbers lexicographically to eliminate deadlocks
        List<String> sortedSeats = new ArrayList<>(requestedSeats);
        Collections.sort(sortedSeats);

        // Fetch & Acquire InnoDB Row Locks (FOR UPDATE)
        List<Seat> lockedSeats = seatRepository.findByShowIdAndSeatNumberInOrderAsc(showId, sortedSeats);

        if (lockedSeats.size() != sortedSeats.size()) {
            seatTakenCounter.increment();
            throw new DomainConflictException("One or more requested seats do not exist");
        }

        for (Seat seat : lockedSeats) {
            if (!"AVAILABLE".equals(seat.getStatus())) {
                seatTakenCounter.increment();
                throw new DomainConflictException("Seat " + seat.getSeatNumber() + " is already taken");
            }
        }

        // 5. Update state atomically
        String reservationId = UUID.randomUUID().toString();
        long totalAmount = show.getPricePaise() * sortedSeats.size();

        for (Seat seat : lockedSeats) {
            seat.setStatus("CONFIRMED");
            seat.setReservationId(reservationId);
        }
        seatRepository.saveAll(lockedSeats);

        Reservation reservation = new Reservation(reservationId, showId, userId, totalAmount, "CONFIRMED");
        reservationRepository.save(reservation);

        ReservationResponse response = new ReservationResponse(
                reservationId, showId, userId, requestedSeats, totalAmount, "confirmed"
        );

        // 6. Record Idempotency Key
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            idempotencyRepository.save(new IdempotencyKey(idempotencyKey, userId, requestHash, reservationId, responseJson));
        } catch (Exception e) {
            throw new RuntimeException("Error serializing response", e);
        }

        confirmedCounter.increment();
        return response;
    }

}