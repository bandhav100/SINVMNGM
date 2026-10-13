package com.smartinventory.purchase.repository;

import com.smartinventory.purchase.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    List<PurchaseOrderItem> findByPurchaseOrderId(Long purchaseOrderId);

    @Query("SELECT COALESCE(SUM(poi.quantity), 0) FROM PurchaseOrderItem poi " +
           "JOIN poi.purchaseOrder po " +
           "WHERE (:companyId IS NULL OR po.company.id = :companyId) " +
           "AND po.status = com.smartinventory.purchase.model.PurchaseOrderStatus.RECEIVED " +
           "AND po.receivedAt >= :startInstant AND po.receivedAt <= :endInstant")
    Long sumReceivedPurchaseUnits(
            @Param("companyId") Long companyId,
            @Param("startInstant") Instant startInstant,
            @Param("endInstant") Instant endInstant
    );

    default Long sumReceivedPurchaseUnits(Instant startInstant, Instant endInstant) {
        return sumReceivedPurchaseUnits(null, startInstant, endInstant);
    }

    @Query("SELECT COALESCE(SUM(poi.totalCost), 0) FROM PurchaseOrderItem poi " +
           "JOIN poi.purchaseOrder po " +
           "WHERE (:companyId IS NULL OR po.company.id = :companyId) " +
           "AND po.status = com.smartinventory.purchase.model.PurchaseOrderStatus.RECEIVED " +
           "AND po.receivedAt >= :startInstant AND po.receivedAt <= :endInstant")
    java.math.BigDecimal sumReceivedPurchaseCost(
            @Param("companyId") Long companyId,
            @Param("startInstant") Instant startInstant,
            @Param("endInstant") Instant endInstant
    );

    default java.math.BigDecimal sumReceivedPurchaseCost(Instant startInstant, Instant endInstant) {
        return sumReceivedPurchaseCost(null, startInstant, endInstant);
    }
}
