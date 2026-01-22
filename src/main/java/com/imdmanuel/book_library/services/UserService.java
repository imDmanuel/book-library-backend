package com.imdmanuel.book_library.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.imdmanuel.book_library.exception.ResourceNotFoundException;
import com.imdmanuel.book_library.mappers.UserMapper;
import com.imdmanuel.book_library.models.User;
import com.imdmanuel.book_library.payload.request.UpdateUserProfileRequest;
import com.imdmanuel.book_library.payload.response.UserResponse;
import com.imdmanuel.book_library.repository.UserRepository;
import com.imdmanuel.book_library.security.services.OidcUserAdapter;
import com.imdmanuel.book_library.security.services.UserDetailsImpl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Gets the currently authenticated user from SecurityContext
     * 
     * @return User entity if authenticated, null otherwise
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        Long userId = null;

        if (principal instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) principal;
            userId = userDetails.getId();
        } else if (principal instanceof OidcUserAdapter) {
            OidcUserAdapter oidcUserAdapter = (OidcUserAdapter) principal;
            userId = oidcUserAdapter.getUserDetails().getId();
        } else {
            return null;
        }

        if (userId != null) {
            return userRepository.findById(userId).orElse(null);
        }
        return null;
    }

    /**
     * Gets the current user or throws an exception if not authenticated
     * 
     * @return User entity
     * @throws RuntimeException if user is not authenticated
     */
    public User getCurrentUserOrThrow() {
        User user = getCurrentUser();
        if (user == null) {
            throw new RuntimeException("User not authenticated");
        }
        return user;
    }

    public UserResponse getCurrentUserResponse() {
        return userMapper.toResponse(getCurrentUserOrThrow());
    }

    public UserResponse updateProfile(@NonNull User currentUser, UpdateUserProfileRequest updateUserProfileRequest) {
        if (updateUserProfileRequest.getEmail() != null && !updateUserProfileRequest.getEmail().isEmpty()) {
            currentUser.setEmail(updateUserProfileRequest.getEmail());
        }

        User updatedUser = userRepository.save(currentUser);

        return userMapper.toResponse(updatedUser);
    }

    public Page<UserResponse> getAllUsers(@NonNull Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    public UserResponse getUserById(@NonNull Long id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
}
