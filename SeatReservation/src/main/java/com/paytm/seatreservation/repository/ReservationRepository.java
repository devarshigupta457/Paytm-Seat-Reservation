package com.paytm.seatreservation.repository;

import com.paytm.seatreservation.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, String> {
}