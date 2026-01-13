package com.imdmanuel.book_library.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReturnBookRequest {
    @NotBlank(message = "loadId is required")
    private Long loanId;
}
