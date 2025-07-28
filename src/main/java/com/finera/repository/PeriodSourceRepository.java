package com.finera.repository;

import com.finera.entities.PeriodSource;
import com.finera.entities.enums.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PeriodSourceRepository extends JpaRepository<PeriodSource, UUID> {

    // Belirli bir döneme ait tüm kaynakları getirmek için bir metot (örnek)
    List<PeriodSource> findByPeriodPeriodId(UUID periodId);

    // Belirli bir kullanıcıya ait tüm kaynakları getirmek için bir metot (örnek)
    List<PeriodSource> findByUserUserId(UUID userId);

    // Başka özel sorgular gerekirse buraya eklenebilir
    Optional<PeriodSource> findByPeriodPeriodIdAndUserUserIdAndSourceNameAndSourceType(
            UUID periodId, UUID userId, String sourceName, SourceType sourceType);
}