package com.imdmanuel.book_library.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.imdmanuel.book_library.payload.request.ReviewRequest;
import com.imdmanuel.book_library.payload.response.MessageResponse;
import com.imdmanuel.book_library.payload.response.ReviewResponse;
import com.imdmanuel.book_library.security.services.UserDetailsImpl;
import com.imdmanuel.book_library.services.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(reviewService.createReview(request, userDetails.getEmail()));
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByBook(
            @PathVariable Long bookId,
            Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByBook(bookId, pageable));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByUser(
            @PathVariable Long userId,
            Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByUser(userId, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(reviewService.updateReview(id, request, userDetails.getEmail()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteReview(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        reviewService.deleteReview(id, userDetails.getEmail());
        return ResponseEntity.ok(new MessageResponse("Review deleted successfully"));
    }
}
