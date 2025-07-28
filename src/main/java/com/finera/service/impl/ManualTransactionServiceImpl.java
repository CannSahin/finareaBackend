package com.finera.service.impl;

import com.finera.dto.ManualTransactionRequestDto;
import com.finera.dto.TransactionResponseDto;
import com.finera.entities.*; // Tüm entity'ler
import com.finera.entities.enums.CategoryType;
import com.finera.entities.enums.SourceType;
import com.finera.exception.BadRequestException; // Yeni exception
import com.finera.exception.ResourceNotFoundException;
import com.finera.repository.*; // Tüm repolar
import com.finera.service.ManualTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManualTransactionServiceImpl implements ManualTransactionService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PeriodRepository periodRepository;
    private final PeriodSourceRepository periodSourceRepository;
    private final TransactionRepository transactionRepository;

    // Manuel girişler için standart kaynak adı öneki
    private static final String MANUAL_SOURCE_NAME_PREFIX = "Manuel Girişler";

    @Override
    @Transactional
    public TransactionResponseDto addManualTransaction(UUID userId, ManualTransactionRequestDto requestDto) {
        log.info("Adding manual transaction for user {} for period {}-{}", userId, requestDto.getYear(), requestDto.getMonth());

        // 1. Kullanıcıyı ve Kategoriyi Bul
        User user = findUserByIdOrThrow(userId);
        Category category = findCategoryByIdOrThrow(requestDto.getCategoryId());

        // 2. Tutarı Kategorinin Türüne Göre Ayarla
        BigDecimal finalAmount = adjustAmountSign(requestDto.getAmount(), category.getCategoryType());

        // 3. Dönemi Bul veya Oluştur
        Period period = findOrCreatePeriod(user, requestDto.getYear(), requestDto.getMonth());

        // 4. Manuel Kaynak Kaydını Bul veya Oluştur
        PeriodSource manualSource = findOrCreateManualSource(period, user);

        // 5. Yeni Transaction Oluştur
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setPeriod(period);
        transaction.setPeriodSource(manualSource); // Manuel kaynağı ata
        transaction.setCategory(category); // Seçilen kategoriyi ata
        transaction.setDescriptionOriginal(requestDto.getDescription()); // Açıklamayı ata
        transaction.setAmount(finalAmount); // İşareti ayarlanmış tutarı ata
        transaction.setCurrency("TRY"); // Varsayılan para birimi
        // İşlem tarihi: Şimdiki zaman veya DTO'dan alınabilir (şimdilik şimdiki zaman)
        transaction.setTransactionDate(OffsetDateTime.now(ZoneOffset.UTC));
        // Diğer alanlar varsayılan veya null olabilir
        transaction.setAiSuggestedCategory(null);
        transaction.setCategorizedByAi(false);

        // 6. Transaction'ı Kaydet
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Manual transaction saved with ID: {}", savedTransaction.getTransactionId());

        // 7. Response DTO'yu oluştur ve döndür
        return mapToTransactionResponseDto(savedTransaction);
    }

    private User findUserByIdOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private Category findCategoryByIdOrThrow(Integer categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
    }

    private BigDecimal adjustAmountSign(BigDecimal requestedAmount, CategoryType categoryType) {
        // Kullanıcı her zaman pozitif girmeli (DTO'da @DecimalMin ile zorlandı)
        if (requestedAmount.signum() <= 0) {
            throw new BadRequestException("Amount must be a positive value."); // Veya farklı handle et
        }
        if (categoryType == CategoryType.EXPENSE) {
            // Giderse, tutarı negatif yap
            return requestedAmount.negate();
        } else {
            // Gelirse, pozitif kalsın
            return requestedAmount;
        }
    }

    private Period findOrCreatePeriod(User user, int year, int month) {
        return periodRepository.findByUserUserIdAndPeriodYearAndPeriodMonth(user.getUserId(), year, month)
                .orElseGet(() -> {
                    log.info("Period {}-{} not found for user {}, creating new one.", year, month, user.getUserId());
                    Period newPeriod = new Period();
                    newPeriod.setUser(user);
                    newPeriod.setPeriodYear(year);
                    newPeriod.setPeriodMonth(month);
                    LocalDate startDate = LocalDate.of(year, month, 1);
                    LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
                    newPeriod.setStartDate(startDate);
                    newPeriod.setEndDate(endDate);
                    return periodRepository.save(newPeriod);
                });
    }

    private PeriodSource findOrCreateManualSource(Period period, User user) {
        String monthName = Month.of(period.getPeriodMonth()).getDisplayName(TextStyle.FULL_STANDALONE, new Locale("tr", "TR"));
        String targetSourceName = MANUAL_SOURCE_NAME_PREFIX + " " + monthName + " " + period.getPeriodYear();

        // Belirli periodId, kullanıcı ve bu isme sahip MANUAL tipinde kaynak var mı?
        return periodSourceRepository.findByPeriodPeriodIdAndUserUserIdAndSourceNameAndSourceType(
                        period.getPeriodId(), user.getUserId(), targetSourceName, SourceType.MANUAL)
                .orElseGet(() -> {
                    log.info("Manual source '{}' not found for period {}, creating new one.", targetSourceName, period.getPeriodId());
                    PeriodSource newSource = new PeriodSource();
                    newSource.setPeriod(period);
                    newSource.setUser(user);
                    newSource.setSourceType(SourceType.MANUAL); // Manuel tip
                    newSource.setSourceName(targetSourceName); // Oluşturulan standart isim
                    newSource.setInstitutionName(null);
                    // flowType kaldırıldığı için set etmeye gerek yok
                    newSource.setUploadTimestamp(null); // Manuel için null olabilir
                    return periodSourceRepository.save(newSource);
                });
    }


    // Helper metot: Transaction entity'sini TransactionResponseDto'ya çevirir
    private TransactionResponseDto mapToTransactionResponseDto(Transaction tx) {
        if (tx == null) return null;
        return TransactionResponseDto.builder()
                .transactionId(tx.getTransactionId())
                .transactionDate(tx.getTransactionDate())
                .descriptionOriginal(tx.getDescriptionOriginal())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .categoryId(tx.getCategory() != null ? tx.getCategory().getCategoryId() : null)
                .categoryNameTr(tx.getCategory() != null ? tx.getCategory().getCategoryNameTr() : null)
                .categoryNameEn(tx.getCategory() != null ? tx.getCategory().getCategoryNameEn() : null)
                .sourceId(tx.getPeriodSource() != null ? tx.getPeriodSource().getSourceId() : null)
                .sourceName(tx.getPeriodSource() != null ? tx.getPeriodSource().getSourceName() : null)
                .periodId(tx.getPeriod() != null ? tx.getPeriod().getPeriodId() : null)
                .userId(tx.getUser() != null ? tx.getUser().getUserId() : null)
                .notes(tx.getNotes())
                .createdAt(tx.getCreatedAt())
                .updatedAt(tx.getUpdatedAt())
                .build();
    }

}