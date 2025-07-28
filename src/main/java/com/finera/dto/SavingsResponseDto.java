package com.finera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavingsResponseDto {
    private String summary; // AI tarafından oluşturulan metinsel özet ve tavsiye
    private List<SavingsRecommendationDto> recommendations; // Yapılandırılmış öneri listesi
}