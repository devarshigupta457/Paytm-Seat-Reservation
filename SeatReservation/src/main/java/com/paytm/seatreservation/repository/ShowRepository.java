package com.paytm.seatreservation.repository;

import com.paytm.seatreservation.model.Show;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowRepository extends JpaRepository<Show, String> {
}