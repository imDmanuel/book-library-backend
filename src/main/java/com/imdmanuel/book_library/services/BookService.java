package com.imdmanuel.book_library.services;

import com.imdmanuel.book_library.exception.ResourceNotFoundException;
import com.imdmanuel.book_library.payload.response.BookResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.imdmanuel.book_library.mappers.BookMapper;
import com.imdmanuel.book_library.models.Book;
import com.imdmanuel.book_library.payload.request.StockUpdateDto;
import com.imdmanuel.book_library.payload.request.UpdateBookRequest;
import com.imdmanuel.book_library.repository.BookRepository;
import com.imdmanuel.book_library.repository.ReviewRepository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final ReviewRepository reviewRepository;

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

    public BookResponse createBook(@NonNull Book book) {
        return bookMapper.toResponse(bookRepository.save(book));
    }

    public BookResponse updateBook(@NonNull Long id, UpdateBookRequest updatedBookRequest) {
        return bookRepository.findById(id).map((existingBook -> {
            bookMapper.updateEntityFromRequest(updatedBookRequest, existingBook);
            return bookMapper.toResponse(bookRepository.save(existingBook));
        })).orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));
    }

    @Transactional
    public void deleteBook(@NonNull Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book", "id", id);
        }
        reviewRepository.deleteByBookId(id);
        bookRepository.deleteById(id);
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
}
