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
}
