package com.imdmanuel.book_library.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.imdmanuel.book_library.enums.LoanStatus;
import com.imdmanuel.book_library.models.User;
import com.imdmanuel.book_library.payload.request.BorrowBookRequest;
import com.imdmanuel.book_library.payload.request.ReturnBookRequest;
import com.imdmanuel.book_library.payload.response.LoanResponse;
import com.imdmanuel.book_library.services.LoanService;
import com.imdmanuel.book_library.services.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final UserService userService;

    @PostMapping("/borrow")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LoanResponse> borrowBook(@RequestBody BorrowBookRequest borrowBookRequest) {
        return ResponseEntity.ok(loanService.borrowBook(borrowBookRequest.getBookId()));
    }

    @PostMapping("/return")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LoanResponse> returnBook(@RequestBody ReturnBookRequest returnBookRequest) {
        return ResponseEntity.ok(loanService.returnBook(returnBookRequest.getLoanId()));
    }

    @GetMapping("/my-loans")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<LoanResponse>> myLoans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "all") LoanStatus status) {
        User currentUser = userService.getCurrentUserOrThrow();

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(loanService.myLoans(currentUser, status, pageable));
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<LoanResponse>> allLoans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "all") LoanStatus status) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(loanService.allLoans(status, pageable));
    }
}
