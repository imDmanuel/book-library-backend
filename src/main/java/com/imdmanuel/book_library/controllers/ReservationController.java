package com.imdmanuel.book_library.controllers;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.imdmanuel.book_library.mappers.ReservationMapper;
import com.imdmanuel.book_library.models.Reservation;
import com.imdmanuel.book_library.models.User;
import com.imdmanuel.book_library.payload.request.CreateReservationRequest;
import com.imdmanuel.book_library.payload.response.ApiResponse;
import com.imdmanuel.book_library.payload.response.MessageResponse;
import com.imdmanuel.book_library.payload.response.PagedResponse;
import com.imdmanuel.book_library.payload.response.ReservationResponse;
import com.imdmanuel.book_library.services.ReservationService;
import com.imdmanuel.book_library.services.UserService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final UserService userService;
    private final ReservationMapper reservationMapper;

    public ReservationController(
            ReservationService reservationService,
            UserService userService,
            ReservationMapper reservationMapper) {
        this.reservationService = reservationService;
        this.userService = userService;
        this.reservationMapper = reservationMapper;
    }

    @PostMapping()
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createReservation(@Valid @RequestBody CreateReservationRequest request) {
        try {
            Reservation reservation = reservationService.createReservation(request.getBookId());
            ReservationResponse response = reservationMapper.toResponse(reservation);

            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> cancelReservation(@PathVariable Long id) {
        try {
            boolean cancelled = reservationService.cancelReservation(id);

            if (cancelled) {
                return ResponseEntity.ok(new MessageResponse("Reservation cancelled successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Reservation not found"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping("my-reservations")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getMyReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "reservationDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        User currentUser = userService.getCurrentUserOrThrow();
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Reservation> reservations = reservationService.getUserReservations(currentUser, pageable);

        List<ReservationResponse> myReservationsList = reservations.getContent().stream()
                .map(reservationMapper::toResponse).collect(Collectors.toList());

        PagedResponse<ReservationResponse> myReservationsResponse = new PagedResponse<>(myReservationsList,
                reservations.getNumber(), reservations.getSize(), reservations.getTotalElements(),
                reservations.getTotalPages(), reservations.hasNext(), reservations.hasPrevious());

        return ResponseEntity.ok(myReservationsResponse);

    }

    @GetMapping("/{id}/position")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getWaitlistPosition(@PathVariable Long id) {
        Optional<Integer> waitlistPositionOptional = reservationService.getWaitlistPosition(id);

        if (waitlistPositionOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Reservation not found or not in waitlist"));
        }

        return ResponseEntity.ok(new ApiResponse<>(waitlistPositionOptional.get()));
    }
}
