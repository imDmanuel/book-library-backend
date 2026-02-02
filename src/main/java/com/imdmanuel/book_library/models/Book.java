package com.imdmanuel.book_library.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Book {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private String author;

    private String title;

    private String isbn;

    private String category;

    private Integer totalCopies;

    private Integer availableCopies;

    private String coverImage;
    private String coverImageId;

    private String documentUrl;
    private String documentId;
}
