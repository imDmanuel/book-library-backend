package com.imdmanuel.book_library.enums;

public enum ReservationStatus {
    PENDING, // Waiting in queue
    ACTIVE, // Book is available, reservation is active
    FULFILLED, // Reservation was fulfilled (book was borrowed)
    EXPIRED, // Reservation expired
    CANCELLED // User cancelled the reservation
}
