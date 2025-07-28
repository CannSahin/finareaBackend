package com.finera.projection;

import java.math.BigDecimal;

public interface CategorySourceProjection {
    String getSourceName();
    String getCategoryNameTr();
    BigDecimal getTotalAmount();
}