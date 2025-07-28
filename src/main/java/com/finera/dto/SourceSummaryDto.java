package com.finera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceSummaryDto {
    private String sourceName; // Belge Türü (İş Bankası, Peşin vs.)
    private List<CategorySummaryDto> categorySummaries;

}