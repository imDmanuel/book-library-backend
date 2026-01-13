package com.imdmanuel.book_library.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.imdmanuel.book_library.enums.ReservationStatus;
import com.imdmanuel.book_library.models.Book;
import com.imdmanuel.book_library.models.Reservation;
import com.imdmanuel.book_library.models.User;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // find active reservation for user and book
    Optional<Reservation> findByUserAndBookAndStatusIn(User user, Book book, List<ReservationStatus> statuses);

    // find all reservations for a user
    Page<Reservation> findByUser(User user, Pageable pageable);

    // find all reservations for a book
    List<Reservation> findByBookAndStatusIn(Book book, List<ReservationStatus> statuses);

    // find pending reservations for a book ordered by position
    List<Reservation> findByBookAndStatusOrderByPositionInQueueAscReservationDateAsc(Book book,
            ReservationStatus status);

    // find the next reservation in queue for a book (position 1)
    Optional<Reservation> findFirstByBookAndStatusOrderByPositionInQueueAscReservationDateAsc(Book book,
            ReservationStatus status);

    // Find expired reservations
    List<Reservation> findExpiredReservations(
            @Param("statuses") List<ReservationStatus> status,
            @Param("now") Date now);

    // count active reservations for a book
    long countByBookAndStatusIn(Book book, List<ReservationStatus> statuses);

    // find user's active reservations
    List<Reservation> findByUserAndSTatusIn(User user, List<ReservationStatus> statuses);
}
