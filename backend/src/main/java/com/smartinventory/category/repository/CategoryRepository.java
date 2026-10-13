package com.smartinventory.category.repository;

import com.smartinventory.category.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByIdAndCompanyId(Long id, Long companyId);

    Page<Category> findAllByCompanyId(Long companyId, Pageable pageable);

    Optional<Category> findByNameIgnoreCase(String name);

    Optional<Category> findByCompanyIdAndNameIgnoreCase(Long companyId, String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCompanyIdAndNameIgnoreCase(Long companyId, String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCompanyIdAndNameIgnoreCaseAndIdNot(Long companyId, String name, Long id);

    @Query("SELECT c FROM Category c WHERE " +
           "(:companyId IS NULL OR c.company.id = :companyId) AND (" +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "(c.description IS NOT NULL AND LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Category> searchCategories(@Param("companyId") Long companyId, @Param("search") String search, Pageable pageable);

    default Page<Category> searchCategories(String search, Pageable pageable) {
        return searchCategories(null, search, pageable);
    }
}
