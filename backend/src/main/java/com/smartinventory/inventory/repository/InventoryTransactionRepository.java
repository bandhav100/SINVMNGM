package com.smartinventory.inventory.repository;

import com.smartinventory.inventory.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    @org.springframework.data.jpa.repository.Query("SELECT it FROM InventoryTransaction it LEFT JOIN FETCH it.performedBy JOIN FETCH it.product WHERE it.referenceType = :referenceType AND it.referenceId = :referenceId")
    List<InventoryTransaction> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);

    boolean existsByReferenceTypeAndReferenceIdAndProductId(String referenceType, Long referenceId, Long productId);

    Page<InventoryTransaction> findByProductId(Long productId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(it) FROM InventoryTransaction it " +
           "WHERE (:companyId IS NULL OR it.company.id = :companyId) " +
           "AND it.createdAt >= :startInstant AND it.createdAt <= :endInstant " +
           "AND (:productId IS NULL OR it.product.id = :productId) " +
           "AND (:transactionType IS NULL OR it.transactionType = :transactionType)")
    long countMovementsInRange(
            @org.springframework.data.repository.query.Param("companyId") Long companyId,
            @org.springframework.data.repository.query.Param("startInstant") java.time.Instant startInstant,
            @org.springframework.data.repository.query.Param("endInstant") java.time.Instant endInstant,
            @org.springframework.data.repository.query.Param("productId") Long productId,
            @org.springframework.data.repository.query.Param("transactionType") com.smartinventory.inventory.model.InventoryTransactionType transactionType
    );

    default long countMovementsInRange(
            java.time.Instant startInstant,
            java.time.Instant endInstant,
            Long productId,
            com.smartinventory.inventory.model.InventoryTransactionType transactionType
    ) {
        return countMovementsInRange(null, startInstant, endInstant, productId, transactionType);
    }

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(it.quantityDelta), 0) FROM InventoryTransaction it " +
           "WHERE (:companyId IS NULL OR it.company.id = :companyId) " +
           "AND it.createdAt >= :startInstant AND it.createdAt <= :endInstant " +
           "AND (:productId IS NULL OR it.product.id = :productId) " +
           "AND (:transactionType IS NULL OR it.transactionType = :transactionType) " +
           "AND it.quantityDelta > 0")
    Long sumInboundUnitsInRange(
            @org.springframework.data.repository.query.Param("companyId") Long companyId,
            @org.springframework.data.repository.query.Param("startInstant") java.time.Instant startInstant,
            @org.springframework.data.repository.query.Param("endInstant") java.time.Instant endInstant,
            @org.springframework.data.repository.query.Param("productId") Long productId,
            @org.springframework.data.repository.query.Param("transactionType") com.smartinventory.inventory.model.InventoryTransactionType transactionType
    );

    default Long sumInboundUnitsInRange(
            java.time.Instant startInstant,
            java.time.Instant endInstant,
            Long productId,
            com.smartinventory.inventory.model.InventoryTransactionType transactionType
    ) {
        return sumInboundUnitsInRange(null, startInstant, endInstant, productId, transactionType);
    }

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(ABS(it.quantityDelta)), 0) FROM InventoryTransaction it " +
           "WHERE (:companyId IS NULL OR it.company.id = :companyId) " +
           "AND it.createdAt >= :startInstant AND it.createdAt <= :endInstant " +
           "AND (:productId IS NULL OR it.product.id = :productId) " +
           "AND (:transactionType IS NULL OR it.transactionType = :transactionType) " +
           "AND it.quantityDelta < 0")
    Long sumOutboundUnitsInRange(
            @org.springframework.data.repository.query.Param("companyId") Long companyId,
            @org.springframework.data.repository.query.Param("startInstant") java.time.Instant startInstant,
            @org.springframework.data.repository.query.Param("endInstant") java.time.Instant endInstant,
            @org.springframework.data.repository.query.Param("productId") Long productId,
            @org.springframework.data.repository.query.Param("transactionType") com.smartinventory.inventory.model.InventoryTransactionType transactionType
    );

    default Long sumOutboundUnitsInRange(
            java.time.Instant startInstant,
            java.time.Instant endInstant,
            Long productId,
            com.smartinventory.inventory.model.InventoryTransactionType transactionType
    ) {
        return sumOutboundUnitsInRange(null, startInstant, endInstant, productId, transactionType);
    }

    @org.springframework.data.jpa.repository.Query(value = "SELECT it FROM InventoryTransaction it " +
           "JOIN FETCH it.product p " +
           "LEFT JOIN FETCH it.performedBy u " +
           "WHERE (:companyId IS NULL OR it.company.id = :companyId) " +
           "AND it.createdAt >= :startInstant AND it.createdAt <= :endInstant " +
           "AND (:productId IS NULL OR p.id = :productId) " +
           "AND (:transactionType IS NULL OR it.transactionType = :transactionType) " +
           "ORDER BY it.createdAt DESC, it.id DESC",
           countQuery = "SELECT COUNT(it) FROM InventoryTransaction it " +
           "WHERE (:companyId IS NULL OR it.company.id = :companyId) " +
           "AND it.createdAt >= :startInstant AND it.createdAt <= :endInstant " +
           "AND (:productId IS NULL OR it.product.id = :productId) " +
           "AND (:transactionType IS NULL OR it.transactionType = :transactionType)")
    Page<InventoryTransaction> findMovementsReport(
            @org.springframework.data.repository.query.Param("companyId") Long companyId,
            @org.springframework.data.repository.query.Param("startInstant") java.time.Instant startInstant,
            @org.springframework.data.repository.query.Param("endInstant") java.time.Instant endInstant,
            @org.springframework.data.repository.query.Param("productId") Long productId,
            @org.springframework.data.repository.query.Param("transactionType") com.smartinventory.inventory.model.InventoryTransactionType transactionType,
            Pageable pageable
    );

    default Page<InventoryTransaction> findMovementsReport(
            java.time.Instant startInstant,
            java.time.Instant endInstant,
            Long productId,
            com.smartinventory.inventory.model.InventoryTransactionType transactionType,
            Pageable pageable
    ) {
        return findMovementsReport(null, startInstant, endInstant, productId, transactionType, pageable);
    }

    @org.springframework.data.jpa.repository.Query("SELECT it FROM InventoryTransaction it JOIN FETCH it.product p WHERE (:companyId IS NULL OR it.company.id = :companyId) ORDER BY it.createdAt DESC")
    List<InventoryTransaction> findTop5RecentTransactions(@org.springframework.data.repository.query.Param("companyId") Long companyId, Pageable pageable);

    default List<InventoryTransaction> findTop5ByOrderByCreatedAtDesc() {
        return findTop5RecentTransactions(null, org.springframework.data.domain.PageRequest.of(0, 5));
    }

    default List<InventoryTransaction> findTop5ByCompanyIdOrderByCreatedAtDesc(Long companyId) {
        return findTop5RecentTransactions(companyId, org.springframework.data.domain.PageRequest.of(0, 5));
    }

    @org.springframework.data.jpa.repository.Query(value = "SELECT it FROM InventoryTransaction it " +
           "JOIN FETCH it.product p " +
           "LEFT JOIN FETCH it.performedBy u " +
           "WHERE (:companyId IS NULL OR it.company.id = :companyId) " +
           "AND (:productId IS NULL OR p.id = :productId) " +
           "AND (:transactionType IS NULL OR it.transactionType = :transactionType) " +
           "AND (:search IS NULL OR :search = '' OR " +
           " LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(it.notes) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(it) FROM InventoryTransaction it JOIN it.product p " +
           "WHERE (:companyId IS NULL OR it.company.id = :companyId) " +
           "AND (:productId IS NULL OR p.id = :productId) " +
           "AND (:transactionType IS NULL OR it.transactionType = :transactionType) " +
           "AND (:search IS NULL OR :search = '' OR " +
           " LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(it.notes) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<InventoryTransaction> searchTransactions(
            @org.springframework.data.repository.query.Param("companyId") Long companyId,
            @org.springframework.data.repository.query.Param("productId") Long productId,
            @org.springframework.data.repository.query.Param("transactionType") com.smartinventory.inventory.model.InventoryTransactionType transactionType,
            @org.springframework.data.repository.query.Param("search") String search,
            Pageable pageable
    );

    default Page<InventoryTransaction> searchTransactions(
            Long productId,
            com.smartinventory.inventory.model.InventoryTransactionType transactionType,
            String search,
            Pageable pageable
    ) {
        return searchTransactions(null, productId, transactionType, search, pageable);
    }
}
