package com.smartinventory.security.jwt;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenRevocationService {

    // In-memory store of revoked JWT IDs (jti) mapped to their expiration date
    private final Map<String, Date> revokedTokens = new ConcurrentHashMap<>();

    public void revokeToken(String jti, Date expiration) {
        if (jti != null && expiration != null) {
            revokedTokens.put(jti, expiration);
            cleanExpiredTokens();
        }
    }

    public boolean isRevoked(String jti) {
        if (jti == null) {
            return false;
        }
        Date expiration = revokedTokens.get(jti);
        if (expiration == null) {
            return false;
        }
        if (expiration.before(new Date())) {
            revokedTokens.remove(jti);
            return false;
        }
        return true;
    }

    private void cleanExpiredTokens() {
        Date now = new Date();
        revokedTokens.entrySet().removeIf(entry -> entry.getValue().before(now));
    }
}
