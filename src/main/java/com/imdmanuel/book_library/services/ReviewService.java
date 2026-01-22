package com.imdmanuel.book_library.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.imdmanuel.book_library.exception.ResourceNotFoundException;
import com.imdmanuel.book_library.mappers.ReviewMapper;
import com.imdmanuel.book_library.models.Book;
import com.imdmanuel.book_library.models.Review;
import com.imdmanuel.book_library.models.User;
import com.imdmanuel.book_library.payload.request.ReviewRequest;
import com.imdmanuel.book_library.payload.response.ReviewResponse;
import com.imdmanuel.book_library.repository.BookRepository;
import com.imdmanuel.book_library.repository.ReviewRepository;
import com.imdmanuel.book_library.repository.UserRepository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    @Transactional
    public ReviewResponse createReview(ReviewRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", request.getBookId()));

        // Check if user already reviewed this book
        reviewRepository.findByBookIdAndUserId(book.getId(), user.getId())
                .ifPresent(r -> {
                    throw new RuntimeException("You have already reviewed this book");
                });

        Review review = reviewMapper.toEntity(request);
        review.setUser(user);
        review.setBook(book);

        Review savedReview = reviewRepository.save(review);
        return reviewMapper.toResponse(savedReview);
    }

    public Page<ReviewResponse> getReviewsByBook(Long bookId, Pageable pageable) {
        return reviewRepository.findByBookId(bookId, pageable)
                .map(reviewMapper::toResponse);
    }

    public Page<ReviewResponse> getReviewsByUser(Long userId, Pageable pageable) {
        return reviewRepository.findByUserId(userId, pageable)
                .map(reviewMapper::toResponse);
    }

    @Transactional
    public ReviewResponse updateReview(@NonNull Long id, ReviewRequest request, String email) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));

        if (!review.getUser().getEmail().equals(email)) {
            throw new RuntimeException("You can only update your own reviews");
        }

        reviewMapper.updateEntityFromRequest(request, review);
        Review updatedReview = reviewRepository.save(review);
        return reviewMapper.toResponse(updatedReview);
    }

    @Transactional
    public void deleteReview(Long id, String email) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));

        if (!review.getUser().getEmail().equals(email)) {
            throw new RuntimeException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
    }

    public Double getAverageRating(Long bookId) {
        Double avg = reviewRepository.getAverageRatingByBookId(bookId);
        return avg != null ? avg : 0.0;
    }

    public Integer getTotalReviews(Long bookId) {
        return reviewRepository.countByBookId(bookId);
    }
}
