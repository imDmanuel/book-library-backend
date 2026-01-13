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
import com.imdmanuel.book_library.payload.response.ApiResponse;
import com.imdmanuel.book_library.payload.response.UserResponse;
import com.imdmanuel.book_library.services.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/current-user")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getCurrentUser() {
        User user = userService.getCurrentUserOrThrow();

        // Convert User entity to UserResponse DTO (excludes password)
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles(),
                user.getStrikes(),
                user.getSuspensionUntil(),
                user.isSuspended());

        return ResponseEntity.ok(new ApiResponse<>(userResponse));
    }

    @PutMapping("/update-profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateUserProfileRequest updateUserProfileRequest) {
        User currentUser = userService.getCurrentUserOrThrow();

        User updatedUser = userService.updateProfile(currentUser, updateUserProfileRequest);

        return ResponseEntity.ok(new ApiResponse<>(new UserResponse(updatedUser.getId(), updatedUser.getUsername(),
                updatedUser.getEmail(), updatedUser.getRoles(), updatedUser.getStrikes(),
                updatedUser.getSuspensionUntil(), updatedUser.isSuspended())));
    }
}
