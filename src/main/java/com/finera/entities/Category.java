package com.finera.entities;

import com.finera.entities.enums.CategoryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "categories", schema = "public")
public class Category {

    @Id
    @Column(name = "category_id", nullable = false, updatable = false)
    private Integer categoryId;

    @NotBlank(message = "Turkish category name cannot be blank")
    @Column(name = "category_name_tr", nullable = false, unique = true, length = 100)
    private String categoryNameTr;

    @NotBlank(message = "English category name cannot be blank")
    @Column(name = "category_name_en", nullable = false, unique = true, length = 100)
    private String categoryNameEn;

    @NotNull(message = "Category type cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = false, length = 10)
    private CategoryType categoryType;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Transaction> transactions;

    @OneToMany(mappedBy = "aiSuggestedCategory", fetch = FetchType.LAZY)
    private List<Transaction> aiSuggestedTransactions;
}