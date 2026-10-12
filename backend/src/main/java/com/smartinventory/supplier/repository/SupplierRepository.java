package com.smartinventory.supplier.repository;

import com.smartinventory.supplier.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByActiveTrue();

    @Query("SELECT s FROM Supplier s WHERE " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "(s.contactPerson IS NOT NULL AND LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           "(s.email IS NOT NULL AND LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           "(s.phone IS NOT NULL AND LOWER(s.phone) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Supplier> searchSuppliers(@Param("search") String search, Pageable pageable);

    @Query("SELECT s FROM Supplier s WHERE s.active = :active AND (" +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "(s.contactPerson IS NOT NULL AND LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           "(s.email IS NOT NULL AND LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Supplier> searchSuppliersByActive(@Param("search") String search, @Param("active") Boolean active, Pageable pageable);

    Page<Supplier> findByActive(Boolean active, Pageable pageable);
}
