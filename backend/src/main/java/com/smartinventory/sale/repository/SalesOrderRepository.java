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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT so FROM SalesOrder so WHERE so.id = :id")
    Optional<SalesOrder> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT COUNT(so) FROM SalesOrder so WHERE so.status = com.smartinventory.sale.model.SalesOrderStatus.COMPLETED " +
           "AND so.completedAt >= :startInstant AND so.completedAt <= :endInstant")
    long countCompletedOrdersInRange(@Param("startInstant") java.time.Instant startInstant, @Param("endInstant") java.time.Instant endInstant);

    @Query(value = "SELECT so FROM SalesOrder so " +
           "WHERE so.status = com.smartinventory.sale.model.SalesOrderStatus.COMPLETED " +
           "AND so.completedAt >= :startInstant AND so.completedAt <= :endInstant " +
           "AND (:search IS NULL OR :search = '' OR " +
           " LOWER(so.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(so.customerName) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(so) FROM SalesOrder so " +
           "WHERE so.status = com.smartinventory.sale.model.SalesOrderStatus.COMPLETED " +
           "AND so.completedAt >= :startInstant AND so.completedAt <= :endInstant " +
           "AND (:search IS NULL OR :search = '' OR " +
           " LOWER(so.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(so.customerName) LIKE LOWER(CONCAT('%', :search, '%')))")
    org.springframework.data.domain.Page<SalesOrder> searchCompletedOrdersReport(
            @Param("startInstant") java.time.Instant startInstant,
            @Param("endInstant") java.time.Instant endInstant,
            @Param("search") String search,
            org.springframework.data.domain.Pageable pageable
    );
}
