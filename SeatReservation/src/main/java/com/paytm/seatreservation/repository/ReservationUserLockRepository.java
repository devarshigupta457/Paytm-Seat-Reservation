package com.paytm.seatreservation.repository;

import com.paytm.seatreservation.model.ReservationUserLock;
import com.paytm.seatreservation.model.ReservationUserLockId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ReservationUserLockRepository
        extends JpaRepository<
        ReservationUserLock,
        ReservationUserLockId> {

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO reservation_user_locks
            (show_id, user_id)
            VALUES (:showId, :userId)
            """,
            nativeQuery = true)
    int createIfNotExists(
            @Param("showId") String showId,
            @Param("userId") String userId
    );


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT l
            FROM ReservationUserLock l
            WHERE l.showId = :showId
              AND l.userId = :userId
            """)
    ReservationUserLock findLockForUpdate(
            @Param("showId") String showId,
            @Param("userId") String userId
    );
}