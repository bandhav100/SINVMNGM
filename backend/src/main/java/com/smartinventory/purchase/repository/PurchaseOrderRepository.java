package com.smartinventory.purchase.repository;

import com.smartinventory.purchase.entity.PurchaseOrder;
import com.smartinventory.purchase.model.PurchaseOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT po FROM PurchaseOrder po WHERE po.id = :id")
    Optional<PurchaseOrder> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT po FROM PurchaseOrder po " +
           "JOIN po.supplier s " +
           "WHERE (:search IS NULL OR :search = '' OR " +
           "       LOWER(po.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "       LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR po.status = :status) " +
           "AND (:supplierId IS NULL OR s.id = :supplierId) " +
           "AND (cast(:startDate as date) IS NULL OR po.expectedDeliveryDate >= :startDate) " +
           "AND (cast(:endDate as date) IS NULL OR po.expectedDeliveryDate <= :endDate)")
    Page<PurchaseOrder> searchPurchaseOrders(
            @Param("search") String search,
            @Param("status") PurchaseOrderStatus status,
            @Param("supplierId") Long supplierId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("SELECT po.status, COUNT(po) FROM PurchaseOrder po " +
           "WHERE po.createdAt >= :startInstant AND po.createdAt <= :endInstant " +
           "GROUP BY po.status")
    java.util.List<Object[]> countOrdersByStatusInRange(
            @Param("startInstant") java.time.Instant startInstant,
            @Param("endInstant") java.time.Instant endInstant
    );

    @Query("SELECT po.supplier.id, po.supplier.name, COUNT(po), " +
           "SUM(CASE WHEN po.status = com.smartinventory.purchase.model.PurchaseOrderStatus.RECEIVED THEN 1 ELSE 0 END), " +
           "COALESCE(SUM(CASE WHEN po.status = com.smartinventory.purchase.model.PurchaseOrderStatus.RECEIVED THEN po.totalAmount ELSE 0 END), 0) " +
           "FROM PurchaseOrder po " +
           "WHERE po.createdAt >= :startInstant AND po.createdAt <= :endInstant " +
           "GROUP BY po.supplier.id, po.supplier.name " +
           "ORDER BY COALESCE(SUM(CASE WHEN po.status = com.smartinventory.purchase.model.PurchaseOrderStatus.RECEIVED THEN po.totalAmount ELSE 0 END), 0) DESC")
    java.util.List<Object[]> getSupplierPurchaseSummaries(
            @Param("startInstant") java.time.Instant startInstant,
            @Param("endInstant") java.time.Instant endInstant
    );

    @Query(value = "SELECT po FROM PurchaseOrder po JOIN FETCH po.supplier s " +
           "WHERE po.createdAt >= :startInstant AND po.createdAt <= :endInstant " +
           "AND (:status IS NULL OR po.status = :status) " +
           "AND (:supplierId IS NULL OR s.id = :supplierId) " +
           "AND (:search IS NULL OR :search = '' OR " +
           " LOWER(po.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(po) FROM PurchaseOrder po JOIN po.supplier s " +
           "WHERE po.createdAt >= :startInstant AND po.createdAt <= :endInstant " +
           "AND (:status IS NULL OR po.status = :status) " +
           "AND (:supplierId IS NULL OR s.id = :supplierId) " +
           "AND (:search IS NULL OR :search = '' OR " +
           " LOWER(po.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<PurchaseOrder> searchPurchaseOrdersReport(
            @Param("startInstant") java.time.Instant startInstant,
            @Param("endInstant") java.time.Instant endInstant,
            @Param("status") PurchaseOrderStatus status,
            @Param("supplierId") Long supplierId,
            @Param("search") String search,
            Pageable pageable
    );
}
