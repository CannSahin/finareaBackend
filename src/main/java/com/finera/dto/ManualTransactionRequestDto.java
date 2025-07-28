package com.finera.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ManualTransactionRequestDto {

    @NotNull(message = "Year cannot be null")
    @Min(value = 1900, message = "Year must be valid")
    private Integer year;

    @NotNull(message = "Month cannot be null")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;

    @NotNull(message = "Category ID cannot be null")
    private Integer categoryId; // Seçilecek kategorinin ID'si

    @NotBlank(message = "Description cannot be blank")
    @Size(max = 255, message = "Description cannot exceed 255 characters") // Uygun bir boyut
    private String description; // Hem kaynak adı hem işlem açıklaması için kullanılabilir

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be positive") // Kullanıcı pozitif girsin, işaretini kategoriden alacağız
    private BigDecimal amount; // Kullanıcı pozitif girer, tipi kategori belirler


}