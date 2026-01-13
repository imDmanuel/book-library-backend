package com.imdmanuel.book_library.controllers;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.imdmanuel.book_library.mappers.BookMapper;
import com.imdmanuel.book_library.models.Book;
import com.imdmanuel.book_library.payload.request.CreateBookRequest;
import com.imdmanuel.book_library.payload.request.StockUpdateDto;
import com.imdmanuel.book_library.payload.request.UpdateBookRequest;
import com.imdmanuel.book_library.payload.response.ApiResponse;
import com.imdmanuel.book_library.payload.response.BookResponse;
import com.imdmanuel.book_library.payload.response.MessageResponse;
import com.imdmanuel.book_library.payload.response.PagedResponse;
import com.imdmanuel.book_library.services.BookService;

import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private BookService bookService;

    private final BookMapper bookMapper;

    public BookController(BookService bookService, BookMapper bookMapper) {
        this.bookService = bookService;
        this.bookMapper = bookMapper;
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Book> books = bookService.searchBooks(title, author, isbn, category, keyword, pageable);

        List<BookResponse> booksResponse = books.getContent().stream().map(bookMapper::toResponse)
                .collect(Collectors.toList());

        PagedResponse<BookResponse> response = new PagedResponse<>(
                booksResponse,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.hasNext(),
                books.hasPrevious());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBookById(@PathVariable Long id) {
        Optional<Book> bookOptional = bookService.getBookById(id);
        if (bookOptional.isPresent()) {
            BookResponse bookResponse = bookMapper.toResponse(bookOptional.get());

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(bookResponse));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Book with ID " + id + " not found"));
        }
    }

    @GetMapping()
    public ResponseEntity<PagedResponse<BookResponse>> getBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Book> books = bookService.getBooks(pageable);

        List<BookResponse> getBooksResponse = books.getContent().stream().map(bookMapper::toResponse)
                .collect(Collectors.toList());

        PagedResponse<BookResponse> response = new PagedResponse<BookResponse>(
                getBooksResponse,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.hasNext(),
                books.hasPrevious());

        return ResponseEntity.ok(response);
    }

    @PostMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createBook(@Valid @RequestBody CreateBookRequest createBookRequest) {

        Book book = bookMapper.toEntity(createBookRequest);

        Book savedBook = bookService.createBook(book);
        BookResponse savedBookResponse = bookMapper.toResponse(savedBook);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(savedBookResponse));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBook(@PathVariable Long id,
            @Valid @RequestBody UpdateBookRequest updateBookRequest) {
        // Book book = new Book(id, updateBookRequest.getAuthor(),
        // updateBookRequest.getTitle(),
        // updateBookRequest.getIsbn(), updateBookRequest.getCategory(),
        // updateBookRequest.getTotalCopies(),
        // updateBookRequest.getAvailableCopies(), updateBookRequest.getCoverImage());

        Optional<Book> updatedBookOptional = bookService.updateBook(id, updateBookRequest);

        if (updatedBookOptional.isPresent()) {
            BookResponse updatedBookResponse = bookMapper.toResponse(updatedBookOptional.get());
            return ResponseEntity.status(HttpStatus.OK).body(updatedBookResponse);
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND).body(new MessageResponse("Book with ID " + id + " not found"));
        }

    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("{id}")
    public ResponseEntity<MessageResponse> deleteBook(@PathVariable Long id) {
        boolean deleted = bookService.deleteBook(id);

        if (deleted) {
            return ResponseEntity.ok(new MessageResponse("Book with ID " + id + " deleted successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Book with ID " + id + " not found"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/stock")
    public ResponseEntity<?> updateStock(@PathVariable Long id, @Valid @RequestBody StockUpdateDto stockUpdateDto) {
        Optional<Book> updatedBook = bookService.updateStock(id, stockUpdateDto);

        if (updatedBook.isPresent()) {
            BookResponse updatedBookResponse = bookMapper.toResponse(updatedBook.get());
            return ResponseEntity.ok(new ApiResponse<>(updatedBookResponse));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Book to update stock with ID " + id + " not found"));
        }
    }
}
