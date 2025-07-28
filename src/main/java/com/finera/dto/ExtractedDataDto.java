package com.finera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedDataDto {
    private int periodYear;
    private int periodMonth; // 1-12
    private List<TransactionDto> transactions;
}

