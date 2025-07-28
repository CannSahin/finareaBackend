package com.finera.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    @JsonFormat(pattern = "yyyy-MM-dd") // AI'dan beklenen tarih formatı
    private LocalDate date;
    private String description;
    private BigDecimal amount;
    private String categoryName;
}