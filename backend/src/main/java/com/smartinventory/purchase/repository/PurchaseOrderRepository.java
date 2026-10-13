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

    Optional<PurchaseOrder> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByOrderNumber(String orderNumber);

    boolean existsByOrderNumberAndCompanyId(String orderNumber, Long companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT po FROM PurchaseOrder po WHERE po.id = :id AND (:companyId IS NULL OR po.company.id = :companyId)")
    Optional<PurchaseOrder> findByIdAndCompanyIdWithLock(@Param("id") Long id, @Param("companyId") Long companyId);

    default Optional<PurchaseOrder> findByIdWithLock(Long id) {
        return findByIdAndCompanyIdWithLock(id, null);
    }

    @Query("SELECT po FROM PurchaseOrder po " +
           "JOIN po.supplier s " +
           "WHERE (:companyId IS NULL OR po.company.id = :companyId) AND (" +
           "       :search IS NULL OR :search = '' OR " +
           "       LOWER(po.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "       LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR po.status = :status) " +
           "AND (:supplierId IS NULL OR s.id = :supplierId) " +
           "AND (cast(:startDate as date) IS NULL OR po.expectedDeliveryDate >= :startDate) " +
           "AND (cast(:endDate as date) IS NULL OR po.expectedDeliveryDate <= :endDate)")
    Page<PurchaseOrder> searchPurchaseOrders(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            @Param("status") PurchaseOrderStatus status,
            @Param("supplierId") Long supplierId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    default Page<PurchaseOrder> searchPurchaseOrders(
            String search,
            PurchaseOrderStatus status,
            Long supplierId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        return searchPurchaseOrders(null, search, status, supplierId, startDate, endDate, pageable);
    }

    @Query("SELECT po.status, COUNT(po) FROM PurchaseOrder po " +
           "WHERE (:companyId IS NULL OR po.company.id = :companyId) " +
           "AND po.createdAt >= :startInstant AND po.createdAt <= :endInstant " +
           "GROUP BY po.status")
    java.util.List<Object[]> countOrdersByStatusInRange(
            @Param("companyId") Long companyId,
            @Param("startInstant") java.time.Instant startInstant,
            @Param("endInstant") java.time.Instant endInstant
    );

    default java.util.List<Object[]> countOrdersByStatusInRange(
            java.time.Instant startInstant,
            java.time.Instant endInstant
    ) {
        return countOrdersByStatusInRange(null, startInstant, endInstant);
    }

    @Query("SELECT po.supplier.id, po.supplier.name, COUNT(po), " +
           "SUM(CASE WHEN po.status = com.smartinventory.purchase.model.PurchaseOrderStatus.RECEIVED THEN 1 ELSE 0 END), " +
           "COALESCE(SUM(CASE WHEN po.status = com.smartinventory.purchase.model.PurchaseOrderStatus.RECEIVED THEN po.totalAmount ELSE 0 END), 0) " +
           "FROM PurchaseOrder po " +
           "WHERE (:companyId IS NULL OR po.company.id = :companyId) " +
           "AND po.createdAt >= :startInstant AND po.createdAt <= :endInstant " +
           "GROUP BY po.supplier.id, po.supplier.name " +
           "ORDER BY COALESCE(SUM(CASE WHEN po.status = com.smartinventory.purchase.model.PurchaseOrderStatus.RECEIVED THEN po.totalAmount ELSE 0 END), 0) DESC")
    java.util.List<Object[]> getSupplierPurchaseSummaries(
            @Param("companyId") Long companyId,
            @Param("startInstant") java.time.Instant startInstant,
            @Param("endInstant") java.time.Instant endInstant
    );

    default java.util.List<Object[]> getSupplierPurchaseSummaries(
            java.time.Instant startInstant,
            java.time.Instant endInstant
    ) {
        return getSupplierPurchaseSummaries(null, startInstant, endInstant);
    }

    @Query(value = "SELECT po FROM PurchaseOrder po JOIN FETCH po.supplier s " +
           "WHERE (:companyId IS NULL OR po.company.id = :companyId) " +
           "AND po.createdAt >= :startInstant AND po.createdAt <= :endInstant " +
           "AND (:status IS NULL OR po.status = :status) " +
           "AND (:supplierId IS NULL OR s.id = :supplierId) " +
           "AND (:search IS NULL OR :search = '' OR " +
           " LOWER(po.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(po) FROM PurchaseOrder po JOIN po.supplier s " +
           "WHERE (:companyId IS NULL OR po.company.id = :companyId) " +
           "AND po.createdAt >= :startInstant AND po.createdAt <= :endInstant " +
           "AND (:status IS NULL OR po.status = :status) " +
           "AND (:supplierId IS NULL OR s.id = :supplierId) " +
           "AND (:search IS NULL OR :search = '' OR " +
           " LOWER(po.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<PurchaseOrder> searchPurchaseOrdersReport(
            @Param("companyId") Long companyId,
            @Param("startInstant") java.time.Instant startInstant,
            @Param("endInstant") java.time.Instant endInstant,
            @Param("status") PurchaseOrderStatus status,
            @Param("supplierId") Long supplierId,
            @Param("search") String search,
            Pageable pageable
    );

    default Page<PurchaseOrder> searchPurchaseOrdersReport(
            java.time.Instant startInstant,
            java.time.Instant endInstant,
            PurchaseOrderStatus status,
            Long supplierId,
            String search,
            Pageable pageable
    ) {
        return searchPurchaseOrdersReport(null, startInstant, endInstant, status, supplierId, search, pageable);
    }

    long countByStatus(PurchaseOrderStatus status);

    long countByCompanyId(Long companyId);

    long countByCompanyIdAndStatus(Long companyId, PurchaseOrderStatus status);

    @Query("SELECT COALESCE(SUM(po.totalAmount), 0) FROM PurchaseOrder po WHERE (:companyId IS NULL OR po.company.id = :companyId) AND po.status = :status")
    java.math.BigDecimal sumTotalAmountByStatus(@Param("companyId") Long companyId, @Param("status") PurchaseOrderStatus status);

    default java.math.BigDecimal sumTotalAmountByStatus(PurchaseOrderStatus status) {
        return sumTotalAmountByStatus(null, status);
    }

    @Query("SELECT po FROM PurchaseOrder po WHERE (:companyId IS NULL OR po.company.id = :companyId) ORDER BY po.createdAt DESC")
    java.util.List<PurchaseOrder> findTop5RecentOrders(@Param("companyId") Long companyId, Pageable pageable);

    default java.util.List<PurchaseOrder> findTop5ByOrderByCreatedAtDesc() {
        return findTop5RecentOrders(null, org.springframework.data.domain.PageRequest.of(0, 5));
    }

    default java.util.List<PurchaseOrder> findTop5ByCompanyIdOrderByCreatedAtDesc(Long companyId) {
        return findTop5RecentOrders(companyId, org.springframework.data.domain.PageRequest.of(0, 5));
    }
}
