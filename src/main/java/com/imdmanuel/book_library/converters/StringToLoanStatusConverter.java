package com.imdmanuel.book_library.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.imdmanuel.book_library.enums.LoanStatus;

@Component
public class StringToLoanStatusConverter implements Converter<String, LoanStatus> {

    @Override
    public LoanStatus convert(@NonNull String source) {
        return LoanStatus.valueOf(source.toUpperCase());
    }
}