package com.imdmanuel.book_library.payload.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockUpdateDto {
    private Integer availableCopies;
    private Integer totalCopies;
}
