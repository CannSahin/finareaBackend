package com.finera.service.ai;

import com.finera.dto.ExtractedDataDto;
import com.finera.exception.AiProcessingException;

public interface AiExtractorService {
    /**
     * Verilen metinden finansal dönem ve işlem verilerini çıkarır.
     * @param statementText Banka ekstresi metni.
     * @return Çıkarılan verileri içeren DTO.
     * @throws AiProcessingException AI işlemi sırasında hata olursa.
     */
    ExtractedDataDto extractData(String statementText);
}