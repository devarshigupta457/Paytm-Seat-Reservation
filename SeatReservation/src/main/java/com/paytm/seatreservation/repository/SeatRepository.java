package com.paytm.seatreservation.repository;

import com.paytm.seatreservation.model.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByShowId(String showId);

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.showId = :showId AND s.reservationId IN " +
            "(SELECT r.id FROM Reservation r WHERE r.userId = :userId AND r.showId = :showId AND r.status = 'CONFIRMED')")
    long countByShowIdAndReservationUserId(@Param("showId") String showId, @Param("userId") String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.showId = :showId AND s.seatNumber IN :seats ORDER BY s.seatNumber ASC")
    List<Seat> findByShowIdAndSeatNumberInOrderAsc(@Param("showId") String showId, @Param("seats") List<String> seats);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           SELECT s
           FROM Seat s
           WHERE s.id IN :seatIds
           ORDER BY s.id
           """)
    List<Seat> findAllByIdForUpdate(@Param("seatIds") List<Long> seatIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM Seat s
        WHERE s.reservationId = :reservationId
        ORDER BY s.seatNumber ASC
        """)
    List<Seat> findByReservationIdForUpdate(
            @Param("reservationId") String reservationId);

    @Modifying
    @Query("""
    UPDATE Seat s
       SET s.status = 'CONFIRMED',
           s.reservationId = :reservationId
     WHERE s.showId = :showId
       AND s.seatNumber = :seatNumber
       AND s.status = 'AVAILABLE'
""")
    int confirmSeat(
            @Param("showId") String showId,
            @Param("seatNumber") String seatNumber,
            @Param("reservationId") String reservationId);
}