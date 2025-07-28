package com.finera.service; // Veya uygun bir paket adı

import com.finera.dto.FileUploadResponseDto;
import com.finera.service.ai.AiProvider;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface PdfProcessingService {

    /**
     * Yüklenen bir PDF banka ekstresini işler.
     * Metni çıkarır, AI kullanarak verileri analiz eder ve işlemleri veritabanına kaydeder.
     *
     * @param file Yüklenen PDF dosyası.
     * @param userId Dosyayı yükleyen kullanıcının ID'si.
     * @param sourceNamePrefix Kaynak adı için isteğe bağlı önek (örn: "Garanti Bonus").
     * @return İşlem sonucunu ve kaydedilen işlem sayısını içeren bir DTO.
     * @throws RuntimeException İşlem sırasında bir hata oluşursa (dosya okuma, AI, veritabanı).
     */
    FileUploadResponseDto processPdfStatement(
            MultipartFile file,
            UUID userId,
            String sourceNamePrefix,
            AiProvider provider // <-- AI Sağlayıcı parametresi eklendi
    );

}