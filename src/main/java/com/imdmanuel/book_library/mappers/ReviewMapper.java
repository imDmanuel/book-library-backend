package com.imdmanuel.book_library.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.imdmanuel.book_library.models.Review;
import com.imdmanuel.book_library.payload.request.ReviewRequest;
import com.imdmanuel.book_library.payload.response.ReviewResponse;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ReviewMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "book", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Review toEntity(ReviewRequest request);

    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "bookTitle", source = "book.title")
    ReviewResponse toResponse(Review review);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "book", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(ReviewRequest request, @MappingTarget Review review);
}
