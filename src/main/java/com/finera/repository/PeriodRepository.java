package com.finera.repository;

import com.finera.entities.Period;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PeriodRepository extends JpaRepository<Period, UUID> {

    /**
     * Belirtilen kullanıcı ID'si, yıl ve ay bilgisine göre Period nesnesini bulur.
     * findOrCreatePeriod metodunda kullanılır.
     *
     * @param userId Kullanıcı ID'si.
     * @param year Dönem yılı.
     * @param month Dönem ayı (1-12).
     * @return Bulunan Period nesnesini içeren Optional, bulunamazsa Optional.empty().
     */
    Optional<Period> findByUserUserIdAndPeriodYearAndPeriodMonth(UUID userId, int year, int month);

    // İleride ihtiyaç duyulabilecek başka sorgu metotları buraya eklenebilir
    // Örneğin: List<Period> findByUserUserIdOrderByPeriodYearDescPeriodMonthDesc(UUID userId);
}