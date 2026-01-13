package com.imdmanuel.book_library.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {
    private Long id;
    private String author;
    private String title;
    private String isbn;
    private String category;
    private Integer totalCopies;
    private Integer availableCopies;
    private String coverImage;
    private Double averageRating;
    private Integer totalReviews;
}
