package com.imdmanuel.book_library.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.imdmanuel.book_library.models.User;
import com.imdmanuel.book_library.payload.request.UpdateUserProfileRequest;
import com.imdmanuel.book_library.payload.response.UserResponse;
import com.imdmanuel.book_library.services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/current-user")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUserResponse());
    }

    @PutMapping("/update-profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserResponse> updateProfile(
            @Valid @RequestBody UpdateUserProfileRequest updateUserProfileRequest) {
        User currentUser = userService.getCurrentUserOrThrow();
        return ResponseEntity.ok(userService.updateProfile(currentUser, updateUserProfileRequest));
    }
}
