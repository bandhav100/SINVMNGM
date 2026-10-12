package com.smartinventory.settings.repository;

import com.smartinventory.settings.entity.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {

    @Query("SELECT s FROM SystemSettings s LEFT JOIN FETCH s.updatedBy WHERE s.id = 1")
    Optional<SystemSettings> findSingletonSettings();
}
