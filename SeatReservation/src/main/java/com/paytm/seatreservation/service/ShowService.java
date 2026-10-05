package com.paytm.seatreservation.service;

import com.paytm.seatreservation.dto.*;
import com.paytm.seatreservation.exception.DomainConflictException;
import com.paytm.seatreservation.model.*;
import com.paytm.seatreservation.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShowService {

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;

    public ShowService(ShowRepository showRepository, SeatRepository seatRepository) {
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public ShowResponse createShow(CreateShowRequest request) {
        String showId = UUID.randomUUID().toString();
        Show show = new Show(showId, request.getName(), request.getPrice_paise());
        showRepository.save(show);

        List<Seat> seats = request.getSeats().stream()
                .map(seatNumber -> new Seat(showId, seatNumber))
                .collect(Collectors.toList());
        seatRepository.saveAll(seats);

        return new ShowResponse(showId, show.getName(), request.getSeats(), show.getPricePaise());
    }

    @Transactional(readOnly = true)
    public ShowStateResponse getShowState(String showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new DomainConflictException("Show not found"));

        List<Seat> seats = seatRepository.findByShowId(showId);
        Map<String, String> seatStatusMap = new HashMap<>();
        int availableCount = 0;
        int confirmedCount = 0;

        for (Seat seat : seats) {
            seatStatusMap.put(seat.getSeatNumber(), seat.getStatus().toLowerCase());
            if ("AVAILABLE".equals(seat.getStatus())) {
                availableCount++;
            } else if ("CONFIRMED".equals(seat.getStatus())) {
                confirmedCount++;
            }
        }

        return new ShowStateResponse(
                show.getId(),
                seats.size(),
                availableCount,
                confirmedCount,
                seatStatusMap
        );
    }
}