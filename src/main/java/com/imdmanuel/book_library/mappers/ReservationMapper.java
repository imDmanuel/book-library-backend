package com.imdmanuel.book_library.mappers;

import java.util.Date;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.imdmanuel.book_library.enums.ReservationStatus;
import com.imdmanuel.book_library.models.Reservation;
import com.imdmanuel.book_library.payload.response.ReservationResponse;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "bookTitle", source = "book.title")
    @Mapping(target = "bookAuthor", source = "book.author")
    @Mapping(target = "isExpired", expression = "java(isExpired(reservation))")
    @Mapping(target = "isActive", expression = "java(reservation.getStatus() == com.imdmanuel.book_library.enums.ReservationStatus.ACTIVE)")
    @Mapping(target = "positionInQueue", ignore = true)
    ReservationResponse toResponse(Reservation reservation);

    default Boolean isExpired(Reservation reservation) {
        if (reservation == null || reservation.getExpiryDate() == null) {
            return false;
        }
        return reservation.getExpiryDate().before(new Date())
                && reservation.getStatus() != ReservationStatus.EXPIRED;
    }
}
