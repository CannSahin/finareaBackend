package com.finera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTotalDto {
    private String categoryNameTr;
    private String categoryNameEn;
    private Integer categoryId;
    private BigDecimal netAmount; // O kategorideki net harcama/kazanç (Giderler - Gelirler)
    // Pozitif ise net harcama, negatif ise net gelir (veya tam tersi, nasıl isterseniz)
}