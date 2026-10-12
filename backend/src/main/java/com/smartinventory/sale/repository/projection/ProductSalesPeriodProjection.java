package com.smartinventory.sale.repository.projection;

public interface ProductSalesPeriodProjection {
    Long getProductId();
    String getProductName();
    String getProductSku();
    Integer getCurrentStock();
    Integer getReorderLevel();
    String getCategoryName();
    Long getUnitsSold();
}
