package com.smartinventory.sale.repository;

import com.smartinventory.sale.entity.SalesOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long>, JpaSpecificationExecutor<SalesOrder> {

    boolean existsByOrderNumber(String orderNumber);

    boolean existsByOrderNumberAndCompanyId(String orderNumber, Long companyId);

    Optional<SalesOrder> findByIdAndCompanyId(Long id, Long companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT so FROM SalesOrder so WHERE so.id = :id AND (:companyId IS NULL OR so.company.id = :companyId)")
    Optional<SalesOrder> findByIdAndCompanyIdWithLock(@Param("id") Long id, @Param("companyId") Long companyId);

    default Optional<SalesOrder> findByIdWithLock(Long id) {
        return findByIdAndCompanyIdWithLock(id, null);
    }

    @Query("SELECT COUNT(so) FROM SalesOrder so WHERE (:companyId IS NULL OR so.company.id = :companyId) " +
           "AND so.status = com.smartinventory.sale.model.SalesOrderStatus.COMPLETED " +
           "AND so.completedAt >= :startInstant AND so.completedAt <= :endInstant")
    long countCompletedOrdersInRange(
            @Param("companyId") Long companyId,
            @Param("startInstant") java.time.Instant startInstant,
            @Param("endInstant") java.time.Instant endInstant
    );

    default long countCompletedOrdersInRange(java.time.Instant startInstant, java.time.Instant endInstant) {
        return countCompletedOrdersInRange(null, startInstant, endInstant);
    }

    @Query(value = "SELECT so FROM SalesOrder so " +
           "WHERE (:companyId IS NULL OR so.company.id = :companyId) " +
           "AND so.status = com.smartinventory.sale.model.SalesOrderStatus.COMPLETED " +
           "AND so.completedAt >= :startInstant AND so.completedAt <= :endInstant " +
           "AND (:search IS NULL OR :search = '' OR " +
           " LOWER(so.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(so.customerName) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(so) FROM SalesOrder so " +
           "WHERE (:companyId IS NULL OR so.company.id = :companyId) " +
           "AND so.status = com.smartinventory.sale.model.SalesOrderStatus.COMPLETED " +
           "AND so.completedAt >= :startInstant AND so.completedAt <= :endInstant " +
           "AND (:search IS NULL OR :search = '' OR " +
           " LOWER(so.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(so.customerName) LIKE LOWER(CONCAT('%', :search, '%')))")
    org.springframework.data.domain.Page<SalesOrder> searchCompletedOrdersReport(
            @Param("companyId") Long companyId,
            @Param("startInstant") java.time.Instant startInstant,
            @Param("endInstant") java.time.Instant endInstant,
            @Param("search") String search,
            org.springframework.data.domain.Pageable pageable
    );

    default org.springframework.data.domain.Page<SalesOrder> searchCompletedOrdersReport(
            java.time.Instant startInstant,
            java.time.Instant endInstant,
            String search,
            org.springframework.data.domain.Pageable pageable
    ) {
        return searchCompletedOrdersReport(null, startInstant, endInstant, search, pageable);
    }

    long countByStatus(com.smartinventory.sale.model.SalesOrderStatus status);

    long countByCompanyIdAndStatus(Long companyId, com.smartinventory.sale.model.SalesOrderStatus status);

    @Query("SELECT COALESCE(SUM(so.totalAmount), 0) FROM SalesOrder so WHERE (:companyId IS NULL OR so.company.id = :companyId) AND so.status = :status")
    java.math.BigDecimal sumTotalAmountByStatus(@Param("companyId") Long companyId, @Param("status") com.smartinventory.sale.model.SalesOrderStatus status);

    default java.math.BigDecimal sumTotalAmountByStatus(com.smartinventory.sale.model.SalesOrderStatus status) {
        return sumTotalAmountByStatus(null, status);
    }

    @Query("SELECT so FROM SalesOrder so WHERE (:companyId IS NULL OR so.company.id = :companyId) ORDER BY so.createdAt DESC")
    java.util.List<SalesOrder> findTop5RecentOrders(@Param("companyId") Long companyId, org.springframework.data.domain.Pageable pageable);

    default java.util.List<SalesOrder> findTop5ByOrderByCreatedAtDesc() {
        return findTop5RecentOrders(null, org.springframework.data.domain.PageRequest.of(0, 5));
    }

    default java.util.List<SalesOrder> findTop5ByCompanyIdOrderByCreatedAtDesc(Long companyId) {
        return findTop5RecentOrders(companyId, org.springframework.data.domain.PageRequest.of(0, 5));
    }
}
