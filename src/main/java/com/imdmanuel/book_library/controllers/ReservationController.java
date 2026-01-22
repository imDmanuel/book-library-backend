package com.imdmanuel.book_library.controllers;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.imdmanuel.book_library.models.User;
import com.imdmanuel.book_library.payload.request.CreateReservationRequest;
import com.imdmanuel.book_library.payload.response.MessageResponse;
import com.imdmanuel.book_library.payload.response.ReservationResponse;
import com.imdmanuel.book_library.services.ReservationService;
import com.imdmanuel.book_library.services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final UserService userService;

    @PostMapping()
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody CreateReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.createReservation(request.getBookId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MessageResponse> cancelReservation(@PathVariable Long id) {
        reservationService.cancelReservation(id);
        return ResponseEntity.ok(new MessageResponse("Reservation cancelled successfully"));
    }

    @GetMapping("my-reservations")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<ReservationResponse>> getMyReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "reservationDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        User currentUser = userService.getCurrentUserOrThrow();
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(reservationService.getUserReservations(currentUser, pageable));
    }

    @GetMapping("/{id}/position")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Integer> getWaitlistPosition(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getWaitlistPosition(id));
    }
}
