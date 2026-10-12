package com.smartinventory.inventory.repository;

import com.smartinventory.inventory.entity.StockAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {

    @Query(value = "SELECT sa FROM StockAdjustment sa JOIN FETCH sa.product p LEFT JOIN FETCH sa.adjustedBy u " +
           "WHERE (:productId IS NULL OR p.id = :productId) " +
           "ORDER BY sa.createdAt DESC, sa.id DESC",
           countQuery = "SELECT COUNT(sa) FROM StockAdjustment sa " +
           "WHERE (:productId IS NULL OR sa.product.id = :productId)")
    Page<StockAdjustment> findByProductIdOptional(@Param("productId") Long productId, Pageable pageable);
}
