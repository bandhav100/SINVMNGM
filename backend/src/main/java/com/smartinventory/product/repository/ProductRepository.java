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

    Optional<Product> findByIdAndCompanyId(Long id, Long companyId);

    Page<Product> findAllByCompanyId(Long companyId, Pageable pageable);

    Optional<Product> findBySku(String sku);

    Optional<Product> findByCompanyIdAndSku(Long companyId, String sku);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsByCompanyIdAndSkuIgnoreCase(Long companyId, String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    boolean existsByCompanyIdAndSkuIgnoreCaseAndIdNot(Long companyId, String sku, Long id);

    @Query("SELECT p FROM Product p WHERE " +
           "(:companyId IS NULL OR p.company.id = :companyId) AND (" +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchProducts(@Param("companyId") Long companyId, @Param("search") String search, Pageable pageable);

    default Page<Product> searchProducts(String search, Pageable pageable) {
        return searchProducts(null, search, pageable);
    }

    long countByCategoryId(Long categoryId);

    long countByCompanyIdAndCategoryId(Long companyId, Long categoryId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids ORDER BY p.id ASC")
    java.util.List<Product> findAllByIdInOrderByIdAscForUpdate(@Param("ids") java.util.List<Long> ids);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND (:companyId IS NULL OR p.company.id = :companyId)")
    Optional<Product> findByIdAndCompanyIdWithLock(@Param("id") Long id, @Param("companyId") Long companyId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT COUNT(p) FROM Product p WHERE (:companyId IS NULL OR p.company.id = :companyId)")
    long countTotalProducts(@Param("companyId") Long companyId);

    default long countTotalProducts() {
        return countTotalProducts(null);
    }

    @Query("SELECT COALESCE(SUM(p.quantity), 0) FROM Product p WHERE (:companyId IS NULL OR p.company.id = :companyId)")
    long sumTotalStockUnits(@Param("companyId") Long companyId);

    default long sumTotalStockUnits() {
        return sumTotalStockUnits(null);
    }

    @Query("SELECT COUNT(p) FROM Product p WHERE (:companyId IS NULL OR p.company.id = :companyId) AND p.quantity = 0")
    long countOutOfStock(@Param("companyId") Long companyId);

    default long countOutOfStock() {
        return countOutOfStock(null);
    }

    @Query("SELECT COUNT(p) FROM Product p WHERE (:companyId IS NULL OR p.company.id = :companyId) AND p.quantity > 0 AND p.quantity <= p.reorderLevel")
    long countLowStock(@Param("companyId") Long companyId);

    default long countLowStock() {
        return countLowStock(null);
    }

    @Query("SELECT COUNT(p) FROM Product p WHERE (:companyId IS NULL OR p.company.id = :companyId) AND p.quantity <= p.reorderLevel")
    long countReorderRequired(@Param("companyId") Long companyId);

    default long countReorderRequired() {
        return countReorderRequired(null);
    }

    @Query("SELECT COALESCE(SUM(p.quantity * p.price), 0) FROM Product p WHERE (:companyId IS NULL OR p.company.id = :companyId)")
    java.math.BigDecimal sumTotalRetailValue(@Param("companyId") Long companyId);

    default java.math.BigDecimal sumTotalRetailValue() {
        return sumTotalRetailValue(null);
    }

    @Query("SELECT p FROM Product p WHERE (:companyId IS NULL OR p.company.id = :companyId) AND p.quantity <= p.reorderLevel ORDER BY p.quantity ASC")
    java.util.List<Product> findReorderRequiredProducts(@Param("companyId") Long companyId);

    default java.util.List<Product> findReorderRequiredProducts() {
        return findReorderRequiredProducts(null);
    }

    @Query("SELECT p FROM Product p WHERE (:companyId IS NULL OR p.company.id = :companyId) AND p.quantity = 0 ORDER BY p.name ASC")
    Page<Product> findOutOfStockProducts(@Param("companyId") Long companyId, Pageable pageable);

    default Page<Product> findOutOfStockProducts(Pageable pageable) {
        return findOutOfStockProducts(null, pageable);
    }

    @Query("SELECT p FROM Product p WHERE (:companyId IS NULL OR p.company.id = :companyId) AND p.quantity > 0 AND p.quantity <= p.reorderLevel ORDER BY p.quantity ASC")
    Page<Product> findLowStockProducts(@Param("companyId") Long companyId, Pageable pageable);

    default Page<Product> findLowStockProducts(Pageable pageable) {
        return findLowStockProducts(null, pageable);
    }

    @Query("SELECT p FROM Product p WHERE (:companyId IS NULL OR p.company.id = :companyId) AND p.quantity <= p.reorderLevel ORDER BY p.quantity ASC")
    Page<Product> findAllAlertProducts(@Param("companyId") Long companyId, Pageable pageable);

    default Page<Product> findAllAlertProducts(Pageable pageable) {
        return findAllAlertProducts(null, pageable);
    }

    @Query("SELECT p FROM Product p WHERE (:companyId IS NULL OR p.company.id = :companyId) AND p.quantity <= p.reorderLevel AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY p.quantity ASC")
    Page<Product> searchAlertProducts(@Param("companyId") Long companyId, @Param("search") String search, Pageable pageable);

    default Page<Product> searchAlertProducts(String search, Pageable pageable) {
        return searchAlertProducts(null, search, pageable);
    }

    @Query("SELECT p FROM Product p WHERE (:companyId IS NULL OR p.company.id = :companyId) AND p.quantity = 0 AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY p.name ASC")
    Page<Product> searchOutOfStockProducts(@Param("companyId") Long companyId, @Param("search") String search, Pageable pageable);

    default Page<Product> searchOutOfStockProducts(String search, Pageable pageable) {
        return searchOutOfStockProducts(null, search, pageable);
    }

    @Query("SELECT p FROM Product p WHERE (:companyId IS NULL OR p.company.id = :companyId) AND p.quantity > 0 AND p.quantity <= p.reorderLevel AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY p.quantity ASC")
    Page<Product> searchLowStockProducts(@Param("companyId") Long companyId, @Param("search") String search, Pageable pageable);

    default Page<Product> searchLowStockProducts(String search, Pageable pageable) {
        return searchLowStockProducts(null, search, pageable);
    }

    @Query(value = "SELECT p FROM Product p LEFT JOIN FETCH p.category c WHERE " +
           "(:companyId IS NULL OR p.company.id = :companyId) AND " +
           "(:categoryId IS NULL OR c.id = :categoryId) AND " +
           "(:status IS NULL OR :status = '' OR :status = 'ALL' OR " +
           " (:status = 'OUT_OF_STOCK' AND p.quantity = 0) OR " +
           " (:status = 'LOW_STOCK' AND p.quantity > 0 AND p.quantity <= p.reorderLevel) OR " +
           " (:status = 'IN_STOCK' AND p.quantity > p.reorderLevel)) AND " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(p) FROM Product p LEFT JOIN p.category c WHERE " +
           "(:companyId IS NULL OR p.company.id = :companyId) AND " +
           "(:categoryId IS NULL OR c.id = :categoryId) AND " +
           "(:status IS NULL OR :status = '' OR :status = 'ALL' OR " +
           " (:status = 'OUT_OF_STOCK' AND p.quantity = 0) OR " +
           " (:status = 'LOW_STOCK' AND p.quantity > 0 AND p.quantity <= p.reorderLevel) OR " +
           " (:status = 'IN_STOCK' AND p.quantity > p.reorderLevel)) AND " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchInventoryReport(
            @Param("companyId") Long companyId,
            @Param("categoryId") Long categoryId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable
    );

    default Page<Product> searchInventoryReport(Long categoryId, String status, String search, Pageable pageable) {
        return searchInventoryReport(null, categoryId, status, search, pageable);
    }
}
