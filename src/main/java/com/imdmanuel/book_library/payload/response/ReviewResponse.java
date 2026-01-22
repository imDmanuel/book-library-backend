package com.imdmanuel.book_library.payload.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private Long id;
    private String username;
    private String bookTitle;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
