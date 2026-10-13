package com.smartinventory.sale.repository.projection;

import java.math.BigDecimal;

public interface FastMovingProductProjection {
    Long getProductId();
    String getProductName();
    String getProductSku();
    String getCategoryName();
    Long getUnitsSold();
    BigDecimal getTotalRevenue();
}
