package com.imdmanuel.book_library.mappers;

import org.mapstruct.Mapper;

import com.imdmanuel.book_library.models.User;
import com.imdmanuel.book_library.payload.response.UserResponse;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
