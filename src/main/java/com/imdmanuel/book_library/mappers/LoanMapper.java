package com.imdmanuel.book_library.mappers;

import java.util.Date;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.imdmanuel.book_library.models.Loan;
import com.imdmanuel.book_library.payload.response.LoanResponse;

@Mapper(componentModel = "spring")
public interface LoanMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "bookTitle", source = "book.title")
    @Mapping(target = "overdue", expression = "java(checkIsOverdue(loan))")
    LoanResponse toResponse(Loan loan);

    default Boolean checkIsOverdue(Loan loan) {
        if (loan == null || loan.getReturnDate() != null) {
            return false;
        }
        Date dueDate = loan.getDueDate();
        if (dueDate == null) {
            return false;
        }
        return dueDate.before(new Date());
    }
}
