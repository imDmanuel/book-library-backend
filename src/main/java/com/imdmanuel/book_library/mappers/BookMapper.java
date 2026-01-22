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

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class BookMapper {

    @Autowired
    protected ReviewService reviewService;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "availableCopies", expression = "java(request.getAvailableCopies() != null ? request.getAvailableCopies() : request.getTotalCopies())")
    public abstract Book toEntity(CreateBookRequest request);

    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "totalReviews", ignore = true)
    public abstract BookResponse toResponseBase(Book book);

    public BookResponse toResponse(Book book) {
        if (book == null) {
            return null;
        }
        BookResponse response = toResponseBase(book);
        response.setAverageRating(reviewService.getAverageRating(book.getId()));
        response.setTotalReviews(reviewService.getTotalReviews(book.getId()));
        return response;
    }

    @Mapping(target = "id", ignore = true)
    public abstract void updateEntityFromRequest(UpdateBookRequest request, @MappingTarget Book book);
}
