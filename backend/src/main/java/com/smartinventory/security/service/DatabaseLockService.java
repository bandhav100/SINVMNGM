package com.smartinventory.security.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseLockService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseLockService.class);

    public static final long ADMIN_SETUP_LOCK_KEY = 987654321L;
    public static final long USER_MUTATION_LOCK_KEY = 987654322L;

    private final JdbcTemplate jdbcTemplate;

    public DatabaseLockService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void acquireAdminSetupLock() {
        log.debug("Acquiring PostgreSQL advisory transaction lock for initial admin setup (key: {})", ADMIN_SETUP_LOCK_KEY);
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(" + ADMIN_SETUP_LOCK_KEY + ")");
    }

    public void acquireUserMutationLock() {
        log.debug("Acquiring PostgreSQL advisory transaction lock for user mutations (key: {})", USER_MUTATION_LOCK_KEY);
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(" + USER_MUTATION_LOCK_KEY + ")");
    }
}
