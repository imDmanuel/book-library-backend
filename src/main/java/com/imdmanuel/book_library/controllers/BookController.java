package com.imdmanuel.book_library.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.imdmanuel.book_library.payload.request.CreateBookRequest;
import com.imdmanuel.book_library.payload.request.StockUpdateDto;
import com.imdmanuel.book_library.payload.request.UpdateBookRequest;
import com.imdmanuel.book_library.payload.response.BookResponse;
import com.imdmanuel.book_library.payload.response.MessageResponse;
import com.imdmanuel.book_library.payload.response.FileUploadResult;
import com.imdmanuel.book_library.services.BookService;
import com.imdmanuel.book_library.services.FileStorageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    private final FileStorageService fileStorageService;

    @GetMapping("/search")
    public ResponseEntity<Page<BookResponse>> searchBooks(
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

        return ResponseEntity.ok(bookService.searchBooks(title, author, isbn, category, keyword, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @GetMapping()
    public ResponseEntity<Page<BookResponse>> getBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(bookService.getBooks(pageable));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookResponse> createBook(
            @Valid @RequestPart("book") CreateBookRequest createBookRequest,
            @RequestPart(value = "cover", required = false) MultipartFile cover,
            @RequestPart(value = "document", required = false) MultipartFile document) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookService.createBook(createBookRequest, cover, document));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> updateBook(@PathVariable Long id,
            @Valid @RequestBody UpdateBookRequest updateBookRequest) {
        return ResponseEntity.ok(bookService.updateBook(id, updateBookRequest));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("{id}")
    public ResponseEntity<MessageResponse> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.ok(new MessageResponse("Book with ID " + id + " deleted successfully"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/stock")
    public ResponseEntity<BookResponse> updateStock(@PathVariable Long id,
            @Valid @RequestBody StockUpdateDto stockUpdateDto) {
        return ResponseEntity.ok(bookService.updateStock(id, stockUpdateDto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/cover")
    public ResponseEntity<BookResponse> uploadCover(@PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        FileUploadResult result = fileStorageService.uploadImage(file, "books/covers");
        return ResponseEntity.ok(bookService.updateBookCover(id, result));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/document")
    public ResponseEntity<BookResponse> uploadDocument(@PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        FileUploadResult result = fileStorageService.uploadRawFile(file, "books/documents");
        return ResponseEntity.ok(bookService.updateBookDocument(id, result));
    }
}
