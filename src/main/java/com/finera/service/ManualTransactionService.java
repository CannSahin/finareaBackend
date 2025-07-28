package com.finera.service;

import com.finera.dto.ManualTransactionRequestDto;
import com.finera.dto.TransactionResponseDto;

import java.util.UUID;

public interface ManualTransactionService {

    /**
     * Kullanıcı için belirtilen döneme manuel bir işlem ekler.
     * Gerekirse Period ve PeriodSource kayıtlarını oluşturur/kullanır.
     *
     * @param userId İşlemi yapan kullanıcının ID'si.
     * @param requestDto Eklenecek işlemin detaylarını içeren DTO.
     * @return Oluşturulan işlemin detaylarını içeren DTO.
     */
    TransactionResponseDto addManualTransaction(UUID userId, ManualTransactionRequestDto requestDto);
}