package com.imdmanuel.book_library.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.imdmanuel.book_library.enums.LoanStatus;
import com.imdmanuel.book_library.mappers.LoanMapper;
import com.imdmanuel.book_library.models.Loan;
import com.imdmanuel.book_library.models.User;
import com.imdmanuel.book_library.payload.request.BorrowBookRequest;
import com.imdmanuel.book_library.payload.request.ReturnBookRequest;
import com.imdmanuel.book_library.payload.response.ApiResponse;
import com.imdmanuel.book_library.payload.response.LoanResponse;
import com.imdmanuel.book_library.payload.response.MessageResponse;
import com.imdmanuel.book_library.payload.response.PagedResponse;
import com.imdmanuel.book_library.services.LoanService;
import com.imdmanuel.book_library.services.UserService;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;
    private final UserService userService;
    private final LoanMapper loanMapper;

    public LoanController(LoanService loanService, UserService userService, LoanMapper loanMapper) {
        this.loanService = loanService;
        this.userService = userService;
        this.loanMapper = loanMapper;
    }

    @PostMapping("/borrow")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> borrowBook(@RequestBody BorrowBookRequest borrowBookRequest) {
        Optional<Loan> loanOptional = loanService.borrowBook(borrowBookRequest.getBookId());

        if (loanOptional.isPresent()) {
            LoanResponse loanResponse = loanMapper.toResponse(loanOptional.get());
            return ResponseEntity.ok(new ApiResponse<>(loanResponse));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Book with ID " + borrowBookRequest.getBookId() + " does not exist"));

        }
    }

    @PostMapping("/return")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> returnBook(@RequestBody ReturnBookRequest returnBookRequest) {
        Optional<Loan> loanOptional = loanService.returnBook(returnBookRequest.getLoanId());

        if (loanOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Loan not found."));
        }

        LoanResponse loanResponse = loanMapper.toResponse(loanOptional.get());
        return ResponseEntity.ok(new ApiResponse<>(loanResponse));
    }

    @GetMapping("/my-loans")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> myLoans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "all") LoanStatus status) {
        User currentUser = userService.getCurrentUserOrThrow();

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Loan> userLoans = loanService.myLoans(currentUser, status, pageable);

        List<LoanResponse> myLoansResponse = userLoans.getContent().stream().map(loanMapper::toResponse)
                .collect(Collectors.toList());

        PagedResponse<LoanResponse> userLoansResponse = new PagedResponse<>(myLoansResponse, userLoans.getNumber(),
                userLoans.getSize(), userLoans.getTotalElements(), userLoans.getTotalPages(), userLoans.hasNext(),
                userLoans.hasPrevious());

        return ResponseEntity.ok(userLoansResponse);
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> allLoans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "all") LoanStatus status) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Loan> allLoans = loanService.allLoans(status, pageable);

        List<LoanResponse> allLoanList = allLoans.getContent().stream().map(loanMapper::toResponse)
                .collect(Collectors.toList());

        PagedResponse<LoanResponse> allLoansResponse = new PagedResponse<>(allLoanList, allLoans.getNumber(),
                allLoans.getSize(), allLoans.getTotalElements(), allLoans.getTotalPages(), allLoans.hasNext(),
                allLoans.hasPrevious());

        return ResponseEntity.ok(allLoansResponse);
    }
}
