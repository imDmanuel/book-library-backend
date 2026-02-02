package com.imdmanuel.book_library.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.imdmanuel.book_library.models.Book;
import com.imdmanuel.book_library.payload.request.CreateBookRequest;
import com.imdmanuel.book_library.payload.request.UpdateBookRequest;
import com.imdmanuel.book_library.payload.response.BookResponse;

import org.springframework.beans.factory.annotation.Autowired;
import com.imdmanuel.book_library.services.ReviewService;
import lombok.NonNull;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class BookMapper {

    @Autowired
    protected ReviewService reviewService;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "availableCopies", expression = "java(request.getAvailableCopies() != null ? request.getAvailableCopies() : request.getTotalCopies())")
    @Mapping(target = "coverImageId", ignore = true)
    @Mapping(target = "documentUrl", ignore = true)
    @Mapping(target = "documentId", ignore = true)
    public abstract @NonNull Book toEntity(CreateBookRequest request);

    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "totalReviews", ignore = true)
    public abstract @NonNull BookResponse toResponseBase(@NonNull Book book);

    public @NonNull BookResponse toResponse(@NonNull Book book) {
        BookResponse response = toResponseBase(book);
        response.setAverageRating(reviewService.getAverageRating(book.getId()));
        response.setTotalReviews(reviewService.getTotalReviews(book.getId()));
        return response;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "coverImageId", ignore = true)
    @Mapping(target = "documentUrl", ignore = true)
    @Mapping(target = "documentId", ignore = true)
    public abstract void updateEntityFromRequest(UpdateBookRequest request, @MappingTarget @NonNull Book book);
}
