package com.smartinventory.supplier.repository;

import com.smartinventory.supplier.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByIdAndCompanyId(Long id, Long companyId);

    List<Supplier> findByActiveTrue();

    List<Supplier> findByCompanyIdAndActiveTrue(Long companyId);

    Page<Supplier> findAllByCompanyId(Long companyId, Pageable pageable);

    Page<Supplier> findByCompanyIdAndActive(Long companyId, Boolean active, Pageable pageable);

    Page<Supplier> findByActive(Boolean active, Pageable pageable);

    @Query("SELECT s FROM Supplier s WHERE (:companyId IS NULL OR s.company.id = :companyId) AND (" +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "(s.contactPerson IS NOT NULL AND LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           "(s.email IS NOT NULL AND LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           "(s.phone IS NOT NULL AND LOWER(s.phone) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Supplier> searchSuppliers(@Param("companyId") Long companyId, @Param("search") String search, Pageable pageable);

    default Page<Supplier> searchSuppliers(String search, Pageable pageable) {
        return searchSuppliers(null, search, pageable);
    }

    @Query("SELECT s FROM Supplier s WHERE (:companyId IS NULL OR s.company.id = :companyId) AND s.active = :active AND (" +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "(s.contactPerson IS NOT NULL AND LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           "(s.email IS NOT NULL AND LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Supplier> searchSuppliersByActive(@Param("companyId") Long companyId, @Param("search") String search, @Param("active") Boolean active, Pageable pageable);

    default Page<Supplier> searchSuppliersByActive(String search, Boolean active, Pageable pageable) {
        return searchSuppliersByActive(null, search, active, pageable);
    }
}
