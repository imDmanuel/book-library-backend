package com.imdmanuel.book_library.services;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.imdmanuel.book_library.mappers.BookMapper;
import com.imdmanuel.book_library.models.Book;
import com.imdmanuel.book_library.payload.request.StockUpdateDto;
import com.imdmanuel.book_library.payload.request.UpdateBookRequest;
import com.imdmanuel.book_library.repository.BookRepository;

import lombok.NonNull;

@Service
public class BookService {
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    public BookService(BookRepository bookRepository, BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
    }

    public Optional<Book> getBookById(@NonNull Long id) {
        return bookRepository.findById(id);
    }

    public Page<Book> getBooks(@NonNull Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    public Page<Book> searchBooks(String title, String author, String isbn, String category, String keyword,
            @NonNull Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            return bookRepository.searchByKeyword(keyword, pageable);
        }
        if (isbn != null && !isbn.isEmpty()) {
            return bookRepository.findByIsbn(isbn, pageable);
        }
        if (author != null && !author.isEmpty()) {
            return bookRepository.findByAuthorContainingIgnoreCase(author, pageable);
        }
        if (title != null && !title.isEmpty()) {
            return bookRepository.findByTitleContainingIgnoreCase(title, pageable);
        }
        if (category != null && !category.isEmpty()) {
            return bookRepository.findByTitleContainingIgnoreCase(category, pageable);
        }
        return bookRepository.findAll(pageable);
    }

    public Book createBook(@NonNull Book book) {
        return bookRepository.save(book);
    }

    public Optional<Book> updateBook(@NonNull Long id, UpdateBookRequest updatedBookRequest) {
        return bookRepository.findById(id).map((existingBook -> {
            bookMapper.updateEntityFromRequest(updatedBookRequest, existingBook);

            return bookRepository.save(existingBook);
        }));

    }

    public boolean deleteBook(@NonNull Long id) {
        if (bookRepository.existsById(id)) {
            bookRepository.deleteById(id);
            return true;
        } else {
            return false;
        }
    }

    public Optional<Book> updateStock(@NonNull Long id, StockUpdateDto stockUpdateDto) {
        return bookRepository.findById(id).map((existingBook) -> {
            if (stockUpdateDto.getAvailableCopies() != null) {
                existingBook.setAvailableCopies(stockUpdateDto.getAvailableCopies());
            }

            if (stockUpdateDto.getTotalCopies() != null) {
                existingBook.setTotalCopies(stockUpdateDto.getTotalCopies());
            }

            return bookRepository.save(existingBook);
        });

    }
}
