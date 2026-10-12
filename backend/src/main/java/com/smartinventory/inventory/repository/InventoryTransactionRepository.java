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
}
