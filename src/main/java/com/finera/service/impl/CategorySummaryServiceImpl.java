package com.finera.service.impl;

import com.finera.dto.CategoryTotalDto;
import com.finera.exception.ResourceNotFoundException;
import com.finera.repository.TransactionRepository;
import com.finera.repository.UserRepository;
import com.finera.service.CategorySummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service // Bu servisin bir Spring bean'i olduğunu belirtir
@RequiredArgsConstructor // Constructor injection için
public class CategorySummaryServiceImpl implements CategorySummaryService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository; // Kullanıcı varlığını kontrol etmek için

    @Override
    @Transactional(readOnly = true) // Sadece okuma işlemi olduğu için
    public List<CategoryTotalDto> getNetCategoryTotalsForPeriod(UUID userId, int year, int month) {
        log.info("Fetching net category totals for user: {}, year: {}, month: {}", userId, year, month);

        // Kullanıcının var olup olmadığını kontrol et
        if (!userRepository.existsById(userId)) {
            log.warn("User not found with ID for fetching category totals: {}", userId);
            throw new ResourceNotFoundException("User not found with id: " + userId + " while fetching category totals.");
        }

        // TransactionRepository'deki yeni metodu çağır
        List<CategoryTotalDto> netTotals = transactionRepository.findNetCategoryTotalsByUserAndPeriod(userId, year, month);

        if (netTotals.isEmpty()) {
            log.info("No net category totals found for user: {}, year: {}, month: {}", userId, year, month);
        } else {
            log.info("Found {} net category total entries for user: {}, year: {}, month: {}", netTotals.size(), userId, year, month);
        }

        return netTotals;
    }
}