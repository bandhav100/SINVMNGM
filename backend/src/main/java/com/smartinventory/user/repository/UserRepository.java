package com.smartinventory.user.repository;

import com.smartinventory.user.entity.User;
import com.smartinventory.user.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole(Role role);

    @Query("SELECT COUNT(u) FROM User u WHERE (:companyId IS NULL OR u.company.id = :companyId) AND u.role = :role AND u.isActive = true")
    long countByCompanyIdAndRoleAndIsActiveTrue(@Param("companyId") Long companyId, @Param("role") Role role);

    default long countByRoleAndIsActiveTrue(Role role) {
        return countByCompanyIdAndRoleAndIsActiveTrue(null, role);
    }

    Optional<User> findByIdAndCompanyId(Long id, Long companyId);

    @Query("""
        SELECT u FROM User u
        WHERE (:companyId IS NULL OR u.company.id = :companyId)
          AND (:query IS NULL OR :query = '' 
               OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (:role IS NULL OR u.role = :role)
          AND (:isActive IS NULL OR u.isActive = :isActive)
    """)
    Page<User> searchUsers(
            @Param("companyId") Long companyId,
            @Param("query") String query,
            @Param("role") Role role,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );

    default Page<User> searchUsers(
            String query,
            Role role,
            Boolean isActive,
            Pageable pageable
    ) {
        return searchUsers(null, query, role, isActive, pageable);
    }
}
