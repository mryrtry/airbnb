package org.mryrt.airbnb.auth.service.jwt.blacklist;

import java.time.Instant;

public interface BlacklistService {

    void blacklistToken(String token, String username, Instant expiresAt);

    boolean isTokenBlacklisted(String token);

}