package com.finera.service.impl;

import com.finera.dto.ExtractedDataDto;
import com.finera.dto.FileUploadResponseDto;
import com.finera.dto.TransactionDto;
import com.finera.entities.Period;
import com.finera.entities.PeriodSource;
import com.finera.entities.Transaction;
import com.finera.entities.User;
import com.finera.entities.enums.SourceType; // Enum'unuzu import edin
import com.finera.exception.ResourceNotFoundException;
import com.finera.repository.PeriodRepository;
import com.finera.repository.PeriodSourceRepository;
import com.finera.repository.TransactionRepository;
import com.finera.repository.UserRepository;
import com.finera.service.PdfProcessingService;
import com.finera.service.ai.AiExtractorService; // AI Servis arayüzü
import com.finera.util.PdfTextExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import com.finera.service.ai.AiExtractorService;
import com.finera.service.ai.AiProvider; // Enum'u import et
import com.finera.repository.CategoryRepository;
import com.finera.entities.Category;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfProcessingServiceImpl implements PdfProcessingService {

    private final PdfTextExtractor pdfTextExtractor;
    private final Map<String, AiExtractorService> aiExtractorServices;
    private final UserRepository userRepository;
    private final PeriodRepository periodRepository;
    private final PeriodSourceRepository periodSourceRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional // Tüm işlemler tek bir transaction içinde olmalı
    public FileUploadResponseDto processPdfStatement(MultipartFile file, UUID userId, String sourceNamePrefix, AiProvider provider) {
        log.info("Starting PDF processing for user {} and file {}", userId, file.getOriginalFilename());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String statementText;
        try {
            statementText = pdfTextExtractor.extractText(file);
            if (statementText == null || statementText.isBlank()) {
                throw new IOException("Extracted text is empty.");
            }
        } catch (IOException e) {
            log.error("Failed to extract text from PDF for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to read PDF file: " + e.getMessage(), e);
        }

        AiExtractorService activeExtractor = getAiExtractorService(provider);
        log.info("Using AI extractor implementation: {}", activeExtractor.getClass().getSimpleName());

        // AI ile veriyi çıkar (seçilen servisi kullanarak)
        ExtractedDataDto extractedData = activeExtractor.extractData(statementText);

        Period period = findOrCreatePeriod(user, extractedData.getPeriodYear(), extractedData.getPeriodMonth());
        PeriodSource periodSource = createPeriodSource(period, user, file.getOriginalFilename(), sourceNamePrefix);
        int savedCount = saveTransactions(extractedData.getTransactions(), periodSource, user, period);

        log.info("Successfully processed PDF for user {}. Saved {} transactions for period {} into source {}",
                userId, savedCount, period.getPeriodId(), periodSource.getSourceId());

        return new FileUploadResponseDto(
                "File processed successfully using " + provider,
                periodSource.getSourceName(),
                savedCount
        );
    }
    private AiExtractorService getAiExtractorService(AiProvider provider) {
        // Enum adını küçük harfe çevir (bean adıyla eşleşmesi için)
        String beanName = provider.name().toLowerCase() + "Extractor";
        AiExtractorService service = aiExtractorServices.get(beanName);
        if (service == null) {
            throw new IllegalArgumentException("No AiExtractorService implementation found for provider: " + provider + " (Expected bean name: '" + beanName + "'). Available beans: " + aiExtractorServices.keySet());
        }
        log.info("Selected AI Extractor bean: {}", beanName);
        return service;
    }

    private Period findOrCreatePeriod(User user, int year, int month) {
        return periodRepository.findByUserUserIdAndPeriodYearAndPeriodMonth(user.getUserId(), year, month)
                .orElseGet(() -> {
                    log.info("Period {}-{} not found for user {}, creating new one.", year, month, user.getUserId());
                    Period newPeriod = new Period();
                    newPeriod.setUser(user);
                    newPeriod.setPeriodYear(year);
                    newPeriod.setPeriodMonth(month);
                    // Ayın başlangıç ve bitiş tarihlerini hesapla
                    LocalDate startDate = LocalDate.of(year, month, 1);
                    LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
                    newPeriod.setStartDate(startDate);
                    newPeriod.setEndDate(endDate);
                    return periodRepository.save(newPeriod);
                });
    }

    private PeriodSource createPeriodSource(Period period, User user, String originalFilename, String sourceNamePrefix) {
        PeriodSource source = new PeriodSource();
        source.setPeriod(period);
        source.setUser(user);
        source.setSourceType(SourceType.STATEMENT); // Ekstre tipi olarak işaretle
        // Kaynak adı oluştur (örn: "Ekstre - ankara_sube_ocak_2024.pdf")
        String finalSourceName = (sourceNamePrefix != null ? sourceNamePrefix + " - " : "Ekstre - ") + originalFilename;
        source.setSourceName(finalSourceName);
        // Kurum adı AI tarafından çıkarılabilir veya kullanıcıdan alınabilir (şimdilik null)
        source.setInstitutionName(null);
        source.setUploadTimestamp(OffsetDateTime.now());
        return periodSourceRepository.save(source);
    }

    private int saveTransactions(List<TransactionDto> transactionDtos, PeriodSource source, User user, Period period) {
        List<Transaction> transactionsToSave = new ArrayList<>();
        final Integer OTHER_CATEGORY_ID = 98;
        for (TransactionDto dto : transactionDtos) {
            if (dto.getDate() == null || dto.getDescription() == null || dto.getAmount() == null) {
                log.warn("Skipping transaction with missing data: {}", dto);
                continue;
            }

            Transaction tx = new Transaction();
            tx.setPeriodSource(source);
            tx.setUser(user);
            tx.setPeriod(period);
            Category foundCategory = null;
            if (dto.getCategoryName() != null && !dto.getCategoryName().isBlank()) {
                // AI'dan gelen kategori adıyla DB'de ara (Büyük/küçük harf duyarsız)
                Optional<Category> categoryOpt = categoryRepository.findByCategoryNameTrIgnoreCase(dto.getCategoryName().trim());
                if (categoryOpt.isPresent()) {
                    foundCategory = categoryOpt.get();
                    log.debug("Category '{}' found for description '{}'", dto.getCategoryName(), dto.getDescription());
                } else {
                    // Alternatif: İngilizce isimle de ara
                    // categoryOpt = categoryRepository.findByCategoryNameEnIgnoreCase(dto.getCategoryName().trim());
                    // if(categoryOpt.isPresent()) foundCategory = categoryOpt.get();
                    // else { ... }

                    log.warn("Category '{}' not found in DB for description '{}'. Assigning default.", dto.getCategoryName(), dto.getDescription());
                    // Bulunamazsa varsayılan "Diğer" kategorisini ata (isteğe bağlı)
                    // foundCategory = categoryRepository.findById(OTHER_CATEGORY_ID).orElse(null);
                }
            } else {
                log.warn("AI did not provide category name for description '{}'. Category will be null.", dto.getDescription());
            }
            tx.setCategory(foundCategory);
            tx.setTransactionDate(dto.getDate().atStartOfDay().atOffset(OffsetDateTime.now().getOffset()));
            tx.setDescriptionOriginal(dto.getDescription());
            // AI'dan gelen amount'un işaretine güvenmiyorsanız, burada kontrol edebilirsiniz
            // Örneğin, açıklama "ATM Nakit Çekim" içeriyorsa negatif yap gibi kurallar eklenebilir.
            // Şimdilik AI'nın doğru işareti verdiğini varsayalım.
            tx.setAmount(dto.getAmount());
            tx.setCurrency("TRY"); // Varsayılan veya AI'dan alınabilir
            tx.setAiSuggestedCategory(null);
            tx.setCategorizedByAi(foundCategory != null);

            transactionsToSave.add(tx);
        }
        transactionRepository.saveAll(transactionsToSave);
        return transactionsToSave.size();
    }

}