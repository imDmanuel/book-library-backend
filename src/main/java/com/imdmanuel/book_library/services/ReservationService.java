package com.imdmanuel.book_library.services;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.imdmanuel.book_library.enums.ReservationStatus;
import com.imdmanuel.book_library.exception.ResourceNotFoundException;
import com.imdmanuel.book_library.mappers.ReservationMapper;
import com.imdmanuel.book_library.models.Book;
import com.imdmanuel.book_library.models.Reservation;
import com.imdmanuel.book_library.models.User;
import com.imdmanuel.book_library.payload.response.ReservationResponse;
import com.imdmanuel.book_library.repository.BookRepository;
import com.imdmanuel.book_library.repository.ReservationRepository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReservationService {

    private static final int RESERVATION_EXPIRY_DAYS = 7;

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final UserService userService;
    private final ReservationMapper reservationMapper;

    /**
     * Create a reservation for a book. If book is available, creates ACTIVE
     * reservation.
     * If not available, adds to waitlist with position in queue.
     */
    @Transactional
    public ReservationResponse createReservation(@NonNull Long bookId) {
        User currentUser = userService.getCurrentUserOrThrow();

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", bookId));

        // check if user has a active reservation for this book
        List<ReservationStatus> activeStatuses = Arrays.asList(ReservationStatus.PENDING, ReservationStatus.ACTIVE);
        reservationRepository.findByUserAndBookAndStatusIn(currentUser, book, activeStatuses)
                .ifPresent(r -> {
                    throw new RuntimeException("You already have an active reservation for this book");
                });

        Reservation reservation = new Reservation();
        reservation.setUser(currentUser);
        reservation.setBook(book);
        reservation.setReservationDate(new Date());

        // Calculate expiry date
        Date expiryDate = new Date();
        expiryDate.setTime(expiryDate.getTime() + (RESERVATION_EXPIRY_DAYS * 24 * 60 * 60 * 1000L));
        reservation.setExpiryDate(expiryDate);

        // check if book is available
        if (book.getAvailableCopies() > 0) {
            // Book is available - create ACTIVE reservation
            reservation.setStatus(ReservationStatus.ACTIVE);
            reservation.setPositionInQueue(0); // No queue position needed
        } else {
            // Book is not available, add to waitlist
            reservation.setStatus(ReservationStatus.PENDING);

            // calculate position in queue
            long queueCount = reservationRepository.countByBookAndStatusIn(book,
                    Arrays.asList(ReservationStatus.PENDING));
            int positionInQueue = (int) queueCount + 1;
            reservation.setPositionInQueue(positionInQueue);
        }

        return reservationMapper.toResponse(reservationRepository.save(reservation));
    }

    /** Cancel a reservation */
    @Transactional
    public void cancelReservation(@NonNull Long reservationId) {
        User currentUser = userService.getCurrentUserOrThrow();

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", reservationId));

        // verify ownership
        if (!reservation.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only cancel your own reservation");
        }

        // can only cancel pending or active reservation
        if (reservation.getStatus() != ReservationStatus.PENDING
                && reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new RuntimeException("Cannot cancel a reservation that is " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        // if it was in a queue, update the positions of the other reservations
        if (reservation.getStatus() == ReservationStatus.PENDING && reservation.getPositionInQueue() != null) {
            updateQueuePositions(reservation.getBook());
        }
    }

    /** get a user's reservations */
    public Page<ReservationResponse> getUserReservations(@NonNull User user, Pageable pageable) {
        return reservationRepository.findByUser(user, pageable).map(reservationMapper::toResponse);
    }

    /** Get all reservations for a book (admin only) */
    public List<Reservation> getBookReservations(@NonNull Book book) {
        List<ReservationStatus> activeStatuses = Arrays.asList(ReservationStatus.PENDING, ReservationStatus.ACTIVE);
        return reservationRepository.findByBookAndStatusIn(book, activeStatuses);
    }

    /**
     * Check if book becomes available and fulfill next reservation in queue
     * This should be called when a book is returned
     */
    @Transactional
    public void checkAndFulfillReservations(@NonNull Book book) {
        if (book.getAvailableCopies() < 0) {
            return; // No copies available
        }

        // find the next reservation in queue
        reservationRepository
                .findFirstByBookAndStatusOrderByPositionInQueueAscReservationDateAsc(book, ReservationStatus.PENDING)
                .ifPresent(reservation -> {
                    // Activate the reservation
                    reservation.setStatus(ReservationStatus.ACTIVE);
                    reservation.setPositionInQueue(0);
                    reservationRepository.save(reservation);

                    // update queue positions
                    updateQueuePositions(book);

                    log.info("Reservation {} activated for book {} (user: {})", reservation.getId(), book.getId(),
                            reservation.getUser().getUsername());

                    // TODO: send notification to user that the book is available
                });
    }

    /**
     * Update queue positions after a reservation is cancelled or fulfilled
     */
    private void updateQueuePositions(@NonNull Book book) {
        List<Reservation> pendingReservations = reservationRepository
                .findByBookAndStatusOrderByPositionInQueueAscReservationDateAsc(book, ReservationStatus.PENDING);

        for (int i = 0; i < pendingReservations.size(); i++) {
            Reservation reservation = pendingReservations.get(i);
            reservation.setPositionInQueue(i + 1);
            reservationRepository.save(reservation);
        }
    }

    /**
     * Expire old reservations (scheduled task - runs daily)
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void expireOldReservations() {
        Date now = new Date();
        List<ReservationStatus> statusesToExpire = Arrays.asList(ReservationStatus.PENDING, ReservationStatus.ACTIVE);

        List<Reservation> expiredReservations = reservationRepository.findExpiredReservations(statusesToExpire, now);

        for (Reservation reservation : expiredReservations) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);

            // update queue positions if it was pending
            if (reservation.getPositionInQueue() != null) {
                updateQueuePositions(reservation.getBook());
            }
        }

        log.info("Expired {} reservations", expiredReservations.size());
    }

    /**
     * Get waitlist position for a user's reservation
     */
    public Integer getWaitlistPosition(@NonNull Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", reservationId));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new RuntimeException("Reservation is not in PENDING status");
        }
        return reservation.getPositionInQueue();
    }
}
