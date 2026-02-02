package com.imdmanuel.book_library.services;

import com.imdmanuel.book_library.exception.ResourceNotFoundException;
import com.imdmanuel.book_library.payload.response.BookResponse;

import java.io.IOException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.imdmanuel.book_library.mappers.BookMapper;
import com.imdmanuel.book_library.models.Book;
import com.imdmanuel.book_library.payload.request.CreateBookRequest;
import com.imdmanuel.book_library.payload.request.StockUpdateDto;
import com.imdmanuel.book_library.payload.request.UpdateBookRequest;
import com.imdmanuel.book_library.repository.BookRepository;
import com.imdmanuel.book_library.repository.ReviewRepository;

import com.imdmanuel.book_library.payload.response.FileUploadResult;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final ReviewRepository reviewRepository;
    private final FileStorageService fileStorageService;

    public BookResponse getBookById(@NonNull Long id) {
        return bookRepository.findById(id)
                .map(bookMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));
    }

    public Page<BookResponse> getBooks(@NonNull Pageable pageable) {
        return bookRepository.findAll(pageable).map(bookMapper::toResponse);
    }

    public Page<BookResponse> searchBooks(String title, String author, String isbn, String category, String keyword,
            @NonNull Pageable pageable) {
        Page<Book> books;
        if (keyword != null && !keyword.isEmpty()) {
            books = bookRepository.searchByKeyword(keyword, pageable);
        } else if (isbn != null && !isbn.isEmpty()) {
            books = bookRepository.findByIsbn(isbn, pageable);
        } else if (author != null && !author.isEmpty()) {
            books = bookRepository.findByAuthorContainingIgnoreCase(author, pageable);
        } else if (title != null && !title.isEmpty()) {
            books = bookRepository.findByTitleContainingIgnoreCase(title, pageable);
        } else if (category != null && !category.isEmpty()) {
            books = bookRepository.findByTitleContainingIgnoreCase(category, pageable);
        } else {
            books = bookRepository.findAll(pageable);
        }
        return books.map(bookMapper::toResponse);
    }

    @Transactional
    public BookResponse createBook(CreateBookRequest request, MultipartFile cover,
            MultipartFile document) throws IOException {
        @NonNull
        Book book = bookMapper.toEntity(request);

        if (cover != null && !cover.isEmpty()) {
            FileUploadResult result = fileStorageService.uploadImage(cover, "books/covers");
            book.setCoverImage(result.getUrl());
            book.setCoverImageId(result.getFileId());
        }

        if (document != null && !document.isEmpty()) {
            FileUploadResult result = fileStorageService.uploadRawFile(document, "books/documents");
            book.setDocumentUrl(result.getUrl());
            book.setDocumentId(result.getFileId());
        }

        Book savedBook = bookRepository.save(book);
        return bookMapper.toResponse(java.util.Objects.requireNonNull(savedBook, "Saved book must not be null"));
    }

    public BookResponse updateBook(@NonNull Long id, UpdateBookRequest updatedBookRequest) {
        return bookRepository.findById(id).map((existingBook -> {
            bookMapper.updateEntityFromRequest(updatedBookRequest, existingBook);
            return bookMapper.toResponse(bookRepository.save(existingBook));
        })).orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));
    }

    @Transactional
    public void deleteBook(@NonNull Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));

        String coverId = book.getCoverImageId();
        String docId = book.getDocumentId();

        // 1. Delete database records
        reviewRepository.deleteByBookId(id);
        bookRepository.deleteById(id);

        // 2. Delete files from storage (Database First, Storage Second)
        try {
            if (coverId != null) {
                fileStorageService.deleteFile(coverId);
            }
            if (docId != null) {
                fileStorageService.deleteFile(docId);
            }
        } catch (IOException e) {
            log.error("Failed to clean up files from storage for book ID {}: {}", id, e.getMessage());
        }
    }

    public BookResponse updateStock(@NonNull Long id, StockUpdateDto stockUpdateDto) {
        return bookRepository.findById(id).map((existingBook) -> {
            if (stockUpdateDto.getAvailableCopies() != null) {
                existingBook.setAvailableCopies(stockUpdateDto.getAvailableCopies());
            }
            if (stockUpdateDto.getTotalCopies() != null) {
                existingBook.setTotalCopies(stockUpdateDto.getTotalCopies());
            }
            return bookMapper.toResponse(bookRepository.save(existingBook));
        }).orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));
    }

    @Transactional
    public BookResponse updateBookCover(@NonNull Long id, FileUploadResult uploadResult) {
        return bookRepository.findById(id).map(book -> {
            String oldCoverId = book.getCoverImageId();

            // Update database
            book.setCoverImage(uploadResult.getUrl());
            book.setCoverImageId(uploadResult.getFileId());
            Book savedBook = bookRepository.save(book);

            // Cleanup old file (Storage Second)
            if (oldCoverId != null) {
                try {
                    fileStorageService.deleteFile(oldCoverId);
                } catch (IOException e) {
                    log.error("Failed to delete orphaned cover for book ID {}: {}", id, e.getMessage());
                }
            }
            return bookMapper.toResponse(savedBook);
        }).orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));
    }

    @Transactional
    public BookResponse updateBookDocument(@NonNull Long id, FileUploadResult uploadResult) {
        return bookRepository.findById(id).map(book -> {
            String oldDocId = book.getDocumentId();

            // Update database
            book.setDocumentUrl(uploadResult.getUrl());
            book.setDocumentId(uploadResult.getFileId());
            Book savedBook = bookRepository.save(book);

            // Cleanup old file (Storage Second)
            if (oldDocId != null) {
                try {
                    fileStorageService.deleteFile(oldDocId);
                } catch (IOException e) {
                    log.error("Failed to delete orphaned document for book ID {}: {}", id, e.getMessage());
                }
            }
            return bookMapper.toResponse(savedBook);
        }).orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));
    }
}
