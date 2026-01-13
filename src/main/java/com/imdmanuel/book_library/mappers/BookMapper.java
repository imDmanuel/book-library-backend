package com.imdmanuel.book_library.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.imdmanuel.book_library.models.Book;
import com.imdmanuel.book_library.payload.request.CreateBookRequest;
import com.imdmanuel.book_library.payload.request.UpdateBookRequest;
import com.imdmanuel.book_library.payload.response.BookResponse;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BookMapper {

    @Mapping(target = "id", ignore = true)
    // @Mapping(target = "averageRating", ignore = true)
    // @Mapping(target = "totalReviews", ignore = true)
    @Mapping(target = "availableCopies", expression = "java(request.getAvailableCopies() != null ? request.getAvailableCopies() : request.getTotalCopies())")
    Book toEntity(CreateBookRequest request);

    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "totalReviews", ignore = true)
    BookResponse toResponse(Book book);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(UpdateBookRequest request, @MappingTarget Book book);
}
