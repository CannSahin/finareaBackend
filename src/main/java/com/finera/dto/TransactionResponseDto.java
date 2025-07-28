package com.finera.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponseDto {
    private UUID transactionId;
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ssZ") // ISO 8601 format
    private OffsetDateTime transactionDate;
    private String descriptionOriginal;
    private BigDecimal amount; // Kaydedilen gerçek tutar (+/-)
    private String currency;
    private Integer categoryId;
    private String categoryNameTr; // Kategori adını da eklemek faydalı
    private String categoryNameEn;
    private UUID sourceId;
    private String sourceName; // Hangi kaynaktan geldiği
    private UUID periodId;
    private UUID userId;
    private String notes;
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ssZ")
    private OffsetDateTime createdAt;
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ssZ")
    private OffsetDateTime updatedAt;

}