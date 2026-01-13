package com.imdmanuel.book_library.payload.response;

import java.util.Date;

import com.imdmanuel.book_library.enums.ReservationStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationResponse {
    private Long id;
    private Long userId;
    private String username;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private Date reservationDate;
    private Date expiryDate;
    private ReservationStatus status;
    private Integer positionInQueue;
    private Boolean isExpired;
    private Boolean isActive;
}
