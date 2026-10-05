package com.paytm.seatreservation.controller;

import com.paytm.seatreservation.dto.*;
import com.paytm.seatreservation.service.ReservationService;
import com.paytm.seatreservation.service.ShowService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shows")
public class ShowController {

    private final ShowService showService;
    private final ReservationService reservationService;

    public ShowController(ShowService showService, ReservationService reservationService) {
        this.showService = showService;
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ShowResponse> createShow(@RequestBody CreateShowRequest request) {
        ShowResponse response = showService.createShow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/reserve")
    public ResponseEntity<ReservationResponse> reserveSeats(
            @PathVariable("id") String showId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @RequestBody ReservationRequest request,
            HttpServletRequest httpRequest) {

        String idempotencyKey = (idempotencyKeyHeader != null) ? idempotencyKeyHeader : request.toString();

        // Extract identity from bearer token
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId = authHeader.substring(7);

        ReservationResponse response = reservationService.reserveSeats(showId, userId, idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowStateResponse> getShowState(@PathVariable("id") String showId) {
        return ResponseEntity.ok(showService.getShowState(showId));
    }
}