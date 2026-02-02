package com.imdmanuel.book_library.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.imdmanuel.book_library.models.Book;
import com.imdmanuel.book_library.models.Loan;
import com.imdmanuel.book_library.models.User;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    Optional<Loan> findByUserAndBookAndReturnDateIsNull(User currentUser, Book book);

    Optional<Loan> findByIdAndUser(Long loanId, User user);

    List<Loan> findByUserAndReturnDateIsNullAndDueDateBefore(User user, Date now);

    Page<Loan> findByUser(User user, Pageable pageable);

    Page<Loan> findByUserAndReturnDateIsNull(User user, Pageable pageable);

    Page<Loan> findByUserAndReturnDateIsNotNull(User user, Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE l.user = :user AND l.returnDate IS NULL AND l.dueDate < :now")
    Page<Loan> findByUserAndReturnDateIsNullAndDueDateBefore(@Param("user") User user, @Param("now") Date now,
            Pageable pageable);

    Page<Loan> findByReturnDateIsNullAndDueDateBefore(Date now, Pageable pageable);

    Page<Loan> findByReturnDateIsNull(Pageable pageable);

    Page<Loan> findByReturnDateIsNotNull(Pageable pageable);

    List<Loan> findByReturnDateIsNullAndDueDateBetween(Date start, Date end);

    List<Loan> findByReturnDateIsNullAndDueDateBefore(Date date);
}
