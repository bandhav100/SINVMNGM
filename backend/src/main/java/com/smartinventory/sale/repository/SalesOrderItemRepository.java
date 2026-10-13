package com.smartinventory.sale.repository;

import com.smartinventory.sale.entity.SalesOrderItem;
import com.smartinventory.sale.repository.projection.FastMovingProductProjection;
import com.smartinventory.sale.repository.projection.ProductSalesPeriodProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Long> {

    List<SalesOrderItem> findBySalesOrderId(Long salesOrderId);

    @Query("SELECT soi.product.id AS productId, " +
           "soi.product.name AS productName, " +
           "soi.product.sku AS productSku, " +
           "c.name AS categoryName, " +
           "SUM(soi.quantity) AS unitsSold, " +
           "SUM(soi.lineTotal) AS totalRevenue " +
           "FROM SalesOrderItem soi " +
           "JOIN soi.salesOrder so " +
           "LEFT JOIN soi.product.category c " +
           "WHERE (:companyId IS NULL OR so.company.id = :companyId) " +
           "AND so.status = com.smartinventory.sale.model.SalesOrderStatus.COMPLETED " +
           "AND so.completedAt >= :startInstant AND so.completedAt <= :endInstant " +
           "GROUP BY soi.product.id, soi.product.name, soi.product.sku, c.name " +
           "ORDER BY SUM(soi.quantity) DESC")
    List<FastMovingProductProjection> findFastMovingProducts(
            @Param("companyId") Long companyId,
            @Param("startInstant") Instant startInstant,
            @Param("endInstant") Instant endInstant,
            Pageable pageable
    );

    default List<FastMovingProductProjection> findFastMovingProducts(
            Instant startInstant,
            Instant endInstant,
            Pageable pageable
    ) {
        return findFastMovingProducts(null, startInstant, endInstant, pageable);
    }

    @Query("SELECT p.id AS productId, " +
           "p.name AS productName, " +
           "p.sku AS productSku, " +
           "p.quantity AS currentStock, " +
           "p.reorderLevel AS reorderLevel, " +
           "c.name AS categoryName, " +
           "COALESCE(SUM(CASE WHEN so.status = com.smartinventory.sale.model.SalesOrderStatus.COMPLETED " +
           "AND so.completedAt >= :startInstant AND so.completedAt <= :endInstant THEN soi.quantity ELSE 0 END), 0) AS unitsSold " +
           "FROM Product p " +
           "LEFT JOIN p.category c " +
           "LEFT JOIN SalesOrderItem soi ON soi.product = p " +
           "LEFT JOIN soi.salesOrder so ON so = soi.salesOrder " +
           "WHERE (:companyId IS NULL OR p.company.id = :companyId) " +
           "GROUP BY p.id, p.name, p.sku, p.quantity, p.reorderLevel, c.name " +
           "ORDER BY COALESCE(SUM(CASE WHEN so.status = com.smartinventory.sale.model.SalesOrderStatus.COMPLETED " +
           "AND so.completedAt >= :startInstant AND so.completedAt <= :endInstant THEN soi.quantity ELSE 0 END), 0) ASC, p.quantity DESC")
    List<ProductSalesPeriodProjection> findSlowMovingProducts(
            @Param("companyId") Long companyId,
            @Param("startInstant") Instant startInstant,
            @Param("endInstant") Instant endInstant,
            Pageable pageable
    );

    default List<ProductSalesPeriodProjection> findSlowMovingProducts(
            Instant startInstant,
            Instant endInstant,
            Pageable pageable
    ) {
        return findSlowMovingProducts(null, startInstant, endInstant, pageable);
    }

    @Query("SELECT COUNT(soi) > 0 FROM SalesOrderItem soi " +
           "JOIN soi.salesOrder so " +
           "WHERE soi.product.id = :productId " +
           "AND so.status = com.smartinventory.sale.model.SalesOrderStatus.COMPLETED")
    boolean hasLifetimeCompletedSales(@Param("productId") Long productId);

    @Query("SELECT MAX(so.completedAt) FROM SalesOrderItem soi " +
           "JOIN soi.salesOrder so " +
           "WHERE soi.product.id = :productId " +
           "AND so.status = com.smartinventory.sale.model.SalesOrderStatus.COMPLETED")
    Instant findLastSoldDateByProductId(@Param("productId") Long productId);

    @Query("SELECT COALESCE(SUM(soi.quantity), 0) FROM SalesOrderItem soi " +
           "JOIN soi.salesOrder so " +
           "WHERE (:companyId IS NULL OR so.company.id = :companyId) " +
           "AND so.status = com.smartinventory.sale.model.SalesOrderStatus.COMPLETED " +
           "AND so.completedAt >= :startInstant AND so.completedAt <= :endInstant")
    Long sumCompletedSalesUnits(
            @Param("companyId") Long companyId,
            @Param("startInstant") Instant startInstant,
            @Param("endInstant") Instant endInstant
    );

    default Long sumCompletedSalesUnits(
            Instant startInstant,
            Instant endInstant
    ) {
        return sumCompletedSalesUnits(null, startInstant, endInstant);
    }

    @Query("SELECT COALESCE(SUM(soi.lineTotal), 0) FROM SalesOrderItem soi " +
           "JOIN soi.salesOrder so " +
           "WHERE (:companyId IS NULL OR so.company.id = :companyId) " +
           "AND so.status = com.smartinventory.sale.model.SalesOrderStatus.COMPLETED " +
           "AND so.completedAt >= :startInstant AND so.completedAt <= :endInstant")
    java.math.BigDecimal sumCompletedSalesRevenue(
            @Param("companyId") Long companyId,
            @Param("startInstant") Instant startInstant,
            @Param("endInstant") Instant endInstant
    );

    default java.math.BigDecimal sumCompletedSalesRevenue(
            Instant startInstant,
            Instant endInstant
    ) {
        return sumCompletedSalesRevenue(null, startInstant, endInstant);
    }
}
