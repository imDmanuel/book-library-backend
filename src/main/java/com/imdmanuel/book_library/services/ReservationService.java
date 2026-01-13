package com.imdmanuel.book_library.services;

import java.lang.StackWalker.Option;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.imdmanuel.book_library.enums.ReservationStatus;
import com.imdmanuel.book_library.models.Book;
import com.imdmanuel.book_library.models.Reservation;
import com.imdmanuel.book_library.models.User;
import com.imdmanuel.book_library.repository.BookRepository;
import com.imdmanuel.book_library.repository.ReservationRepository;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ReservationService {

    private static final int RESERVATION_EXPIRY_DAYS = 7;
    private static final int RESERVATION_ACTIVE_HOURS = 48;

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final UserService userService;

    public ReservationService(
            ReservationRepository reservationRepository,
            BookRepository bookRepository,
            UserService userService) {
        this.reservationRepository = reservationRepository;
        this.bookRepository = bookRepository;
        this.userService = userService;
    }

    /**
     * Create a reservation for a book. If book is available, creates ACTIVE
     * reservation.
     * If not available, adds to waitlist with position in queue.
     */
    @Transactional
    public Reservation createReservation(@NonNull Long bookId) {
        User currentUser = userService.getCurrentUserOrThrow();

        Optional<Book> bookOptional = bookRepository.findById(bookId);
        if (bookOptional.isEmpty()) {
            throw new RuntimeException("Book with ID " + bookId + " not found");
        }

        Book book = bookOptional.get();

        // check if user has a active reservation for this book
        List<ReservationStatus> activeStatuses = Arrays.asList(ReservationStatus.PENDING, ReservationStatus.ACTIVE);
        Optional<Reservation> existingReservation = reservationRepository.findByUserAndBookAndStatusIn(currentUser,
                book, activeStatuses);

        if (existingReservation.isPresent()) {
            throw new RuntimeException("You already have an active reservation for this book");
        }

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
            reservation.setPositionQueue(0); // No queue position needed
        } else {
            // Book is not available, add to waitlist
            reservation.setStatus(ReservationStatus.ACTIVE);

            // calculate position in queue
            long queueCount = reservationRepository.countByBookAndStatusIn(book,
                    Arrays.asList(ReservationStatus.PENDING));
            int positionInQueue = (int) queueCount + 1;
            reservation.setPositionQueue(positionInQueue);
        }

        return reservationRepository.save(reservation);
    }

    /** Cancel a reservation */
    @Transactional
    public boolean cancelReservation(@NonNull Long reservationId) {
        User currentUser = userService.getCurrentUserOrThrow();

        Optional<Reservation> reservaOptional = reservationRepository.findById(reservationId);
        if (reservaOptional.isEmpty()) {
            return false;
        }

        Reservation reservation = reservaOptional.get();

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
        if (reservation.getStatus() == ReservationStatus.PENDING && reservation.getPositionQueue() != null) {
            updateQueuePositions(reservation.getBook());
        }

        return true;
    }

    /** get a user's reservations */
    public Page<Reservation> getUserReservations(@NonNull User user, Pageable pageable) {
        return reservationRepository.findByUser(user, pageable);
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
        Optional<Reservation> nextReservation = reservationRepository
                .findFirstByBookAndStatusOrderByPositionInQueueAscReservationDateAsc(book, ReservationStatus.PENDING);

        if (nextReservation.isPresent()) {
            Reservation reservation = nextReservation.get();

            // Activate the reservation
            reservation.setStatus(ReservationStatus.ACTIVE);
            reservation.setPositionQueue(0);
            reservationRepository.save(reservation);

            // update queue positions
            updateQueuePositions(book);

            log.info("Reservation {} activated for book {} (user: {})", reservation.getId(), book.getId(),
                    reservation.getUser().getUsername());

            // TODO: send notification to user that the book is available
        }
    }

    /**
     * Update queue positions after a reservation is cancelled or fulfilled
     */
    private void updateQueuePositions(@NonNull Book book) {
        List<Reservation> pendingReservations = reservationRepository
                .findByBookAndStatusOrderByPositionInQueueAscReservationDateAsc(book, ReservationStatus.PENDING);

        for (int i = 0; i < pendingReservations.size(); i++) {
            Reservation reservation = pendingReservations.get(i);
            reservation.setPositionQueue(i + 1);
            reservationRepository.save(reservation);
        }
    }

    /**
     * Expire old reservations (scheduled task - runs daily)
     */
    @Scheduled(cron = "0 0 0 * ?")
    @Transactional
    public void expireOldReservations() {
        Date now = new Date();
        List<ReservationStatus> statusesToExpire = Arrays.asList(ReservationStatus.PENDING, ReservationStatus.ACTIVE);

        List<Reservation> expiredReservations = reservationRepository.findExpiredReservations(statusesToExpire, now);

        for (Reservation reservation : expiredReservations) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);

            // update queue positions if it was pending
            if (reservation.getPositionQueue() != null) {
                updateQueuePositions(reservation.getBook());
            }
        }

        log.info("Expired {} reservations", expiredReservations.size());
    }

    /**
     * Get waitlist position for a user's reservation
     */
    public Optional<Integer> getWaitlistPosition(@NonNull Long reservationId) {
        Optional<Reservation> reservationOptional = reservationRepository.findById(reservationId);

        if (reservationOptional.isEmpty()) {
            return Optional.empty();
        }

        Reservation reservation = reservationOptional.get();
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            return Optional.empty();
        }
        return Optional.ofNullable(reservation.getPositionQueue());
    }
}
