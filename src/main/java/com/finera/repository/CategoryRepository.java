package com.finera.repository;

import com.finera.entities.Category;
import com.finera.entities.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    Optional<Category> findByCategoryNameTrIgnoreCase(String categoryNameTr);
    

}