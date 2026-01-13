package com.imdmanuel.book_library.services;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.imdmanuel.book_library.enums.LoanStatus;
import com.imdmanuel.book_library.models.Book;
import com.imdmanuel.book_library.models.Loan;
import com.imdmanuel.book_library.models.User;
import com.imdmanuel.book_library.repository.BookRepository;
import com.imdmanuel.book_library.repository.LoanRepository;

import lombok.NonNull;

@Service
public class LoanService {

    private final PenaltyService penaltyService;
    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final UserService userService;
    private final ReservationService reservationService;

    public LoanService(LoanRepository loanRepository, BookRepository bookRepository, UserService userService,
            PenaltyService penaltyService, ReservationService reservationService) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.userService = userService;
        this.penaltyService = penaltyService;
        this.reservationService = reservationService;
    }

    @Transactional
    public Optional<Loan> borrowBook(@NonNull Long bookId) {
        User currentUser = userService.getCurrentUserOrThrow();

        penaltyService.checkSuspensionStatus(currentUser);

        if (currentUser.isSuspended()) {
            if (currentUser.getSuspensionUntil() != null && new Date().before(currentUser.getSuspensionUntil())) {
                throw new RuntimeException("Your account is suspended until " + currentUser.getSuspensionUntil()
                        + ". You cannot borrow books.");
            }
        }

        Optional<Book> bookOptional = bookRepository.findById(bookId);

        if (bookOptional.isEmpty()) {
            return Optional.empty();
        }

        Book book = bookOptional.get();

        if (book.getAvailableCopies() <= 0) {
            throw new RuntimeException("Book is not available. No copies available for borrowing");
        }

        Optional<Loan> existingLoan = loanRepository.findByUserAndBookAndReturnDateIsNull(currentUser, book);
        if (existingLoan.isPresent()) {
            throw new RuntimeException("You have already borrowed this book");
        }

        Loan loan = new Loan();
        loan.setUser(currentUser);
        loan.setBook(book);
        loan.setBorrowDate(new Date());

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_MONTH, 14);
        loan.setDueDate(calendar.getTime());
        loan.setReturnDate(null);

        Loan savedLoan = loanRepository.save(loan);
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        return Optional.of(savedLoan);
    }

    @Transactional
    public Optional<Loan> returnBook(@NonNull Long loanId) {
        User currentUser = userService.getCurrentUserOrThrow();

        Optional<Loan> loanOptional = loanRepository.findByIdAndUser(loanId, currentUser);

        if (loanOptional.isEmpty()) {
            return Optional.empty();
        }

        Loan loan = loanOptional.get();

        if (loan.getReturnDate() != null) {
            throw new RuntimeException("Book has already been returned");
        }

        loan.setReturnDate(new Date());
        Loan savedLoan = loanRepository.save(loan);

        penaltyService.applyReturnPenalty(savedLoan);

        Book book = loan.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

        // TODO: Can this be async?
        // check and fulfill reservations when book is returned
        reservationService.checkAndFulfillReservations(book);

        return Optional.of(savedLoan);
    }

    public Page<Loan> allLoans(LoanStatus status, @NonNull Pageable pageable) {
        Date now = new Date();

        return switch (status) {
            case ALL -> loanRepository.findAll(pageable);
            case ACTIVE -> loanRepository.findByReturnDateIsNull(pageable);
            case RETURNED -> loanRepository.findByReturnDateIsNotNull(pageable);
            case OVERDUE -> loanRepository.findByReturnDateIsNullAndDueDateBefore(now, pageable);
        };

    }

    public Page<Loan> myLoans(User currentUser, LoanStatus status, Pageable pageable) {
        Date now = new Date();

        return switch (status) {
            case ALL -> loanRepository.findByUser(currentUser, pageable);
            case ACTIVE -> loanRepository.findByUserAndReturnDateIsNull(currentUser, pageable);
            case RETURNED -> loanRepository.findByUserAndReturnDateIsNotNull(currentUser, pageable);
            case OVERDUE -> loanRepository.findByUserAndReturnDateIsNullAndDueDateBefore(currentUser, now, pageable);
        };
    }
}
