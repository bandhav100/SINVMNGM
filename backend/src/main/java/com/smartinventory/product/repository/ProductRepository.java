package com.smartinventory.product.repository;

import com.smartinventory.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Product> searchProducts(@Param("search") String search, Pageable pageable);

    long countByCategoryId(Long categoryId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids ORDER BY p.id ASC")
    java.util.List<Product> findAllByIdInOrderByIdAscForUpdate(@Param("ids") java.util.List<Long> ids);

    @Query("SELECT COUNT(p) FROM Product p")
    long countTotalProducts();

    @Query("SELECT COALESCE(SUM(p.quantity), 0) FROM Product p")
    long sumTotalStockUnits();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.quantity = 0")
    long countOutOfStock();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.quantity > 0 AND p.quantity <= p.reorderLevel")
    long countLowStock();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.quantity <= p.reorderLevel")
    long countReorderRequired();

    @Query("SELECT COALESCE(SUM(p.quantity * p.price), 0) FROM Product p")
    java.math.BigDecimal sumTotalRetailValue();

    @Query("SELECT p FROM Product p WHERE p.quantity <= p.reorderLevel ORDER BY p.quantity ASC")
    java.util.List<Product> findReorderRequiredProducts();

    @Query("SELECT p FROM Product p WHERE p.quantity = 0 ORDER BY p.name ASC")
    Page<Product> findOutOfStockProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.quantity > 0 AND p.quantity <= p.reorderLevel ORDER BY p.quantity ASC")
    Page<Product> findLowStockProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.quantity <= p.reorderLevel ORDER BY p.quantity ASC")
    Page<Product> findAllAlertProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.quantity <= p.reorderLevel AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY p.quantity ASC")
    Page<Product> searchAlertProducts(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.quantity = 0 AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY p.name ASC")
    Page<Product> searchOutOfStockProducts(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.quantity > 0 AND p.quantity <= p.reorderLevel AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY p.quantity ASC")
    Page<Product> searchLowStockProducts(@Param("search") String search, Pageable pageable);

    @Query(value = "SELECT p FROM Product p LEFT JOIN FETCH p.category c WHERE " +
           "(:categoryId IS NULL OR c.id = :categoryId) AND " +
           "(:status IS NULL OR :status = '' OR :status = 'ALL' OR " +
           " (:status = 'OUT_OF_STOCK' AND p.quantity = 0) OR " +
           " (:status = 'LOW_STOCK' AND p.quantity > 0 AND p.quantity <= p.reorderLevel) OR " +
           " (:status = 'IN_STOCK' AND p.quantity > p.reorderLevel)) AND " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(p) FROM Product p LEFT JOIN p.category c WHERE " +
           "(:categoryId IS NULL OR c.id = :categoryId) AND " +
           "(:status IS NULL OR :status = '' OR :status = 'ALL' OR " +
           " (:status = 'OUT_OF_STOCK' AND p.quantity = 0) OR " +
           " (:status = 'LOW_STOCK' AND p.quantity > 0 AND p.quantity <= p.reorderLevel) OR " +
           " (:status = 'IN_STOCK' AND p.quantity > p.reorderLevel)) AND " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchInventoryReport(
            @Param("categoryId") Long categoryId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable
    );
}
