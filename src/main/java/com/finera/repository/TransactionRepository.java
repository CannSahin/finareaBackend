package com.finera.repository;

import com.finera.dto.CategoryTotalDto; // Yeni DTO'yu import edin
import com.finera.entities.Transaction;
import com.finera.projection.CategorySourceProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("""
        SELECT
            ps.sourceName AS sourceName,
            c.categoryNameTr AS categoryNameTr,
            SUM(t.amount) AS totalAmount
        FROM
            Transaction t
        JOIN
            t.periodSource ps
        JOIN
            t.category c
        JOIN
            t.period p
        WHERE
            t.user.userId = :userId
            AND p.periodYear = :year
            AND p.periodMonth = :month
            AND c.categoryType = com.finera.entities.enums.CategoryType.EXPENSE
            AND t.amount < 0
        GROUP BY
            ps.sourceName,
            c.categoryNameTr
        ORDER BY
            ps.sourceName,
            c.categoryNameTr
        """)
    List<CategorySourceProjection> findExpenseSummaryByUserAndPeriod(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") int month
    );

    // YENİ METOT BURADA EKLENDİ
    @Query("""
            SELECT new com.finera.dto.CategoryTotalDto(
                c.categoryNameTr,
                c.categoryNameEn,
                c.categoryId,
                SUM(CASE
                        WHEN c.categoryType = com.finera.entities.enums.CategoryType.EXPENSE THEN (CASE WHEN t.amount < 0 THEN t.amount * -1 ELSE t.amount END)
                        WHEN c.categoryType = com.finera.entities.enums.CategoryType.INCOME THEN (CASE WHEN t.amount > 0 THEN t.amount * -1 ELSE t.amount END)
                        ELSE 0
                    END)
            )
            FROM Transaction t
            JOIN t.category c
            JOIN t.period p
            WHERE t.user.userId = :userId
              AND p.periodYear = :year
              AND p.periodMonth = :month
            GROUP BY c.categoryId, c.categoryNameTr, c.categoryNameEn
            ORDER BY c.categoryNameTr ASC
            """)
    List<CategoryTotalDto> findNetCategoryTotalsByUserAndPeriod(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") int month
    );

}