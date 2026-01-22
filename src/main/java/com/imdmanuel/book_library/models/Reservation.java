package com.imdmanuel.book_library.models;

import java.util.Date;

import com.imdmanuel.book_library.enums.ReservationStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "book_id", referencedColumnName = "id", nullable = false)
    private Book book;

    private Date reservationDate;

    private Date expiryDate;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    private Integer positionInQueue; // For waitlist - position 1 is next in line

    @PrePersist
    protected void onCreate() {
        if (reservationDate == null) {
            reservationDate = new Date();
        }
        if (status == null) {
            status = ReservationStatus.PENDING;
        }
        // Set expiry date to 7 days from reservation
        if (expiryDate == null) {
            Date expiry = new Date();
            expiry.setTime(expiry.getTime() + (7 * 24 * 60 * 60 * 1000L)); // 7 days
            expiryDate = expiry;
        }
    }
}
