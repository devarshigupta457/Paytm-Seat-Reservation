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
    private final ReservationUserLockRepository reservationUserLockRepository;

    private final Counter confirmedCounter;
    private final Counter seatTakenCounter;
    private final Counter limitExceededCounter;
    private final Counter idempotencyMismatchCounter;
    private final Counter idempotentReplayCounter;

    public ReservationService(
            ShowRepository showRepository,
            SeatRepository seatRepository,
            ReservationRepository reservationRepository,
            IdempotencyKeyRepository idempotencyRepository,
            ObjectMapper objectMapper,
            MeterRegistry registry,
            ReservationUserLockRepository reservationUserLockRepository) {

        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
        this.reservationRepository = reservationRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
        this.reservationUserLockRepository =
                reservationUserLockRepository;

        this.confirmedCounter =
                registry.counter("reservations.confirmed");

        this.seatTakenCounter =
                registry.counter(
                        "reservations.declined",
                        "reason",
                        "seat_taken"
                );

        this.limitExceededCounter =
                registry.counter(
                        "reservations.declined",
                        "reason",
                        "user_limit_exceeded"
                );

        this.idempotencyMismatchCounter =
                registry.counter(
                        "reservations.declined",
                        "reason",
                        "idempotency_mismatch"
                );

        this.idempotentReplayCounter =
                registry.counter(
                        "reservations.declined",
                        "reason",
                        "idempotent_replay"
                );
    }

    // ============================================================
    // CANCEL RESERVATION
    // ============================================================

    @Transactional
    public ReservationResponse cancelReservation(
            String reservationId,
            String userId) {

        // 1. Find reservation
        Reservation reservation =
                reservationRepository.findById(reservationId)
                        .orElseThrow(() ->
                                new DomainConflictException(
                                        "Reservation not found: "
                                                + reservationId));

        // 2. Only owner can cancel
        if (!reservation.getUserId().equals(userId)) {

            throw new ForbiddenException(
                    "Only the reservation owner may cancel this reservation");
        }

        // 3. Reservation must be CONFIRMED
        if (!"CONFIRMED".equals(reservation.getStatus())) {

            throw new DomainConflictException(
                    "Reservation is already "
                            + reservation.getStatus());
        }

        // 4. Lock all seats belonging to reservation
        List<Seat> lockedSeats =
                seatRepository.findByReservationIdForUpdate(
                        reservationId);

        // 5. Make sure seats exist
        if (lockedSeats.isEmpty()) {

            throw new DomainConflictException(
                    "No seats are associated with reservation: "
                            + reservationId);
        }

        // 6. Safety validation
        for (Seat seat : lockedSeats) {

            if (!reservationId.equals(
                    seat.getReservationId())) {

                throw new DomainConflictException(
                        "Seat "
                                + seat.getSeatNumber()
                                + " is no longer associated with reservation "
                                + reservationId);
            }

            if (!"CONFIRMED".equals(seat.getStatus())) {

                throw new DomainConflictException(
                        "Seat "
                                + seat.getSeatNumber()
                                + " is not in CONFIRMED state");
            }
        }

        // 7. Release seats
        List<String> seatNumbers = new ArrayList<>();

        for (Seat seat : lockedSeats) {

            seatNumbers.add(
                    seat.getSeatNumber());

            seat.setStatus("AVAILABLE");
            seat.setReservationId(null);
        }

        seatRepository.saveAll(lockedSeats);

        // 8. Cancel reservation
        reservation.setStatus("CANCELLED");

        reservationRepository.save(reservation);

        return new ReservationResponse(
                reservation.getId(),
                reservation.getShowId(),
                reservation.getUserId(),
                seatNumbers,
                reservation.getAmountPaise(),
                "cancelled"
        );
    }


    // ============================================================
    // RESERVE SEATS
    // ============================================================

    @Transactional
    public ReservationResponse reserveSeats(
            String showId,
            String userId,
            String idempotencyKey,
            ReservationRequest request) {

        /*
         * ========================================================
         * VALIDATION
         * ========================================================
         */

        if (idempotencyKey == null
                || idempotencyKey.isBlank()) {

            throw new DomainConflictException(
                    "Idempotency-Key header is required");
        }

        if (request == null
                || request.getSeats() == null
                || request.getSeats().isEmpty()) {

            throw new DomainConflictException(
                    "At least one seat must be requested");
        }


        /*
         * ========================================================
         * REQUEST HASH
         * ========================================================
         *
         * Sort before hashing so the same logical request:
         *
         * [A1, A2]
         *
         * and
         *
         * [A2, A1]
         *
         * produce the same hash.
         */

        List<String> requestedSeats =
                new ArrayList<>(request.getSeats());

        List<String> sortedSeats =
                new ArrayList<>(requestedSeats);

        Collections.sort(sortedSeats);

        String requestHash =
                DigestUtils.sha256Hex(
                        String.join(",", sortedSeats)
                );


        /*
         * ========================================================
         * USER LOCK
         * ========================================================
         *
         * This MUST happen BEFORE:
         *
         * - idempotency check
         * - per-user limit check
         *
         * Otherwise 10 parallel requests from the same user
         * can all see the same old count.
         */

        acquireUserLock(showId, userId);


        /*
         * ========================================================
         * IDEMPOTENCY CHECK
         * ========================================================
         *
         * Because the user lock is already held, two concurrent
         * requests from the same user cannot pass this check
         * simultaneously.
         */

        Optional<IdempotencyKey> existingKey =
                idempotencyRepository
                        .findByIdempotencyKeyAndUserId(
                                idempotencyKey,
                                userId
                        );

        if (existingKey.isPresent()) {

            IdempotencyKey ik =
                    existingKey.get();

            /*
             * Same key + different seats = 409
             */
            if (!requestHash.equals(
                    ik.getRequestHash())) {

                idempotencyMismatchCounter.increment();

                throw new DomainConflictException(
                        "Idempotency key reused with different payload");
            }

            /*
             * Same key + same request = return
             * original response.
             */

            idempotentReplayCounter.increment();

            try {

                return objectMapper.readValue(
                        ik.getResponseJson(),
                        ReservationResponse.class
                );

            } catch (Exception e) {

                throw new DomainConflictException(
                        "Unable to restore idempotent reservation response");
            }
        }


        /*
         * ========================================================
         * FETCH SHOW
         * ========================================================
         */

        Show show =
                showRepository.findById(showId)
                        .orElseThrow(() ->
                                new DomainConflictException(
                                        "Show not found: "
                                                + showId));


        /*
         * ========================================================
         * REQUESTED SEATS VALIDATION
         * ========================================================
         */

        if (sortedSeats.size()
                != new HashSet<>(sortedSeats).size()) {

            throw new DomainConflictException(
                    "Duplicate seat requested");
        }


        /*
         * ========================================================
         * PER USER LIMIT
         * ========================================================
         *
         * IMPORTANT:
         *
         * User lock was acquired BEFORE this query.
         *
         * Therefore parallel requests from the same user
         * cannot all read the same old count.
         */

        int perUserLimit = 4;

        long existingCount =
                seatRepository
                        .countByShowIdAndReservationUserId(
                                showId,
                                userId
                        );

        if (existingCount
                + sortedSeats.size()
                > perUserLimit) {

            limitExceededCounter.increment();

            throw new DomainConflictException(
                    "Exceeds maximum limit of "
                            + perUserLimit
                            + " seats per user");
        }


        /*
         * ========================================================
         * LOCK SEATS
         * ========================================================
         *
         * Seat numbers are sorted before locking.
         *
         * This deterministic order greatly reduces deadlocks
         * when multiple requests reserve multiple seats.
         */

        List<Seat> lockedSeats =
                seatRepository
                        .findByShowIdAndSeatNumberInOrderAsc(
                                showId,
                                sortedSeats
                        );


        /*
         * Seat doesn't exist.
         */

        if (lockedSeats.size()
                != sortedSeats.size()) {

            seatTakenCounter.increment();

            throw new DomainConflictException(
                    "One or more requested seats do not exist");
        }


        /*
         * ========================================================
         * CHECK SEAT STATE
         * ========================================================
         *
         * These rows are already locked with FOR UPDATE.
         *
         * Therefore another transaction cannot change them
         * until this transaction commits/rolls back.
         */

        for (Seat seat : lockedSeats) {

            if (!"AVAILABLE".equals(
                    seat.getStatus())) {

                seatTakenCounter.increment();

                throw new DomainConflictException(
                        "Seat "
                                + seat.getSeatNumber()
                                + " is already taken");
            }
        }


        /*
         * ========================================================
         * CREATE RESERVATION ID
         * ========================================================
         */

        String reservationId =
                UUID.randomUUID().toString();

        long totalAmount =
                show.getPricePaise()
                        * sortedSeats.size();


        /*
         * ========================================================
         * CLAIM SEATS
         * ========================================================
         *
         * IMPORTANT:
         *
         * The seats are already locked with SELECT FOR UPDATE.
         *
         * Therefore only one transaction can pass the
         * AVAILABLE check for a particular seat.
         */

        for (Seat seat : lockedSeats) {

            seat.setStatus("CONFIRMED");

            seat.setReservationId(
                    reservationId
            );
        }

        seatRepository.saveAll(lockedSeats);


        /*
         * ========================================================
         * CREATE RESERVATION
         * ========================================================
         */

        Reservation reservation =
                new Reservation(
                        reservationId,
                        showId,
                        userId,
                        totalAmount,
                        "CONFIRMED"
                );

        reservationRepository.save(reservation);


        /*
         * ========================================================
         * RESPONSE
         * ========================================================
         */

        ReservationResponse response =
                new ReservationResponse(
                        reservationId,
                        showId,
                        userId,
                        sortedSeats,
                        totalAmount,
                        "confirmed"
                );


        /*
         * ========================================================
         * SAVE IDEMPOTENCY RECORD
         * ========================================================
         */

        try {

            String responseJson =
                    objectMapper.writeValueAsString(
                            response
                    );

            idempotencyRepository.save(
                    new IdempotencyKey(
                            idempotencyKey,
                            userId,
                            requestHash,
                            reservationId,
                            responseJson
                    )
            );

        } catch (Exception e) {

            throw new DomainConflictException(
                    "Unable to save idempotency record");
        }


        /*
         * ========================================================
         * METRIC
         * ========================================================
         */

        confirmedCounter.increment();

        return response;
    }


    // ============================================================
    // USER LOCK
    // ============================================================

    private void acquireUserLock(
            String showId,
            String userId) {

        /*
         * The repository must create the row if it doesn't exist
         * and then SELECT it FOR UPDATE.
         */
        reservationUserLockRepository
                .createIfNotExists(
                        showId,
                        userId
                );

        reservationUserLockRepository
                .findLockForUpdate(
                        showId,
                        userId
                );
    }
}