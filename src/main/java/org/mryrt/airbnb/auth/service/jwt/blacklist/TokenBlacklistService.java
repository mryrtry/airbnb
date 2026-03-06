package org.mryrt.airbnb.auth.service.jwt.blacklist;

import lombok.extern.slf4j.Slf4j;
import org.mryrt.airbnb.auth.constants.JwtConstants;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class TokenBlacklistService implements BlacklistService {

    private static final int TOKEN_MAP_INITIAL_CAPACITY = 1000;
    private static final float TOKEN_MAP_LOAD_FACTOR = 0.75f;
    private static final int TOKEN_MAP_CONCURRENCY_LEVEL = 32;

    private final ConcurrentHashMap<String, TokenInfo> blacklistedTokens;

    private final AtomicLong totalTokensAdded;
    private final AtomicBoolean cleanupInProgress;
    private volatile long lastCleanupTimestamp;

    public TokenBlacklistService() {
        this.blacklistedTokens = new ConcurrentHashMap<>(TOKEN_MAP_INITIAL_CAPACITY, TOKEN_MAP_LOAD_FACTOR, TOKEN_MAP_CONCURRENCY_LEVEL);
        this.totalTokensAdded = new AtomicLong();
        this.lastCleanupTimestamp = System.currentTimeMillis();
        this.cleanupInProgress = new AtomicBoolean(false);
    }

    @Override
    public void blacklistToken(String token, String username, Instant expiresAt) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        if (isTokenExpired(expiresAt)) {
            log.debug("Token already expired, skipping blacklist for user: {}", username);
            return;
        }

        boolean added = addTokenToBlacklist(token, username, expiresAt);
        if (added) {
            log.debug("Token blacklisted for user: {}, expires: {}", username, expiresAt);
        }

        triggerCleanupIfNeeded();
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        if (token == null) {
            return false;
        }

        TokenInfo tokenInfo = blacklistedTokens.get(token);
        if (tokenInfo == null) {
            return false;
        }

        return validateAndCleanupToken(Map.entry(token, tokenInfo));
    }

    private boolean isTokenExpired(Instant expiresAt) {
        return expiresAt.toEpochMilli() <= System.currentTimeMillis();
    }

    private boolean addTokenToBlacklist(String token, String username, Instant expiresAt) {
        TokenInfo newTokenInfo = new TokenInfo(username, expiresAt.toEpochMilli());
        TokenInfo existingTokenInfo = blacklistedTokens.putIfAbsent(token, newTokenInfo);

        if (existingTokenInfo == null) {
            totalTokensAdded.incrementAndGet();
            return true;
        } else {
            log.warn("Token already blacklisted for user: {}", existingTokenInfo.username());
            return false;
        }
    }

    private void triggerCleanupIfNeeded() {
        if (shouldCleanup() && cleanupInProgress.compareAndSet(false, true)) {
            try {
                cleanupExpiredTokens();
            } finally {
                cleanupInProgress.set(false);
            }
        }
    }

    private boolean shouldCleanup() {
        return System.currentTimeMillis() - lastCleanupTimestamp > JwtConstants.BLACKLIST_CLEANUP_INTERVAL;
    }

    private void cleanupExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        log.debug("Starting blacklist cleanup");

        int initialSize = blacklistedTokens.size();
        long startTime = System.currentTimeMillis();

        int removedCount = removeExpiredTokens(currentTime);

        lastCleanupTimestamp = currentTime;
        log.info("Blacklist cleanup completed: {} -> {} tokens, removed: {}, took {}ms",
                initialSize, blacklistedTokens.size(), removedCount,
                System.currentTimeMillis() - startTime);
    }

    private int removeExpiredTokens(long currentTime) {
        Iterator<Map.Entry<String, TokenInfo>> iterator = blacklistedTokens.entrySet().iterator();
        int removedCount = 0;

        while (iterator.hasNext()) {
            Map.Entry<String, TokenInfo> entry = iterator.next();
            if (isTokenExpired(entry.getValue().expiresAt(), currentTime)) {
                iterator.remove();
                removedCount++;
            }
        }
        return removedCount;
    }

    private boolean isTokenExpired(long expiresAt, long currentTime) {
        return currentTime > expiresAt;
    }

    private boolean validateAndCleanupToken(Map.Entry<String, TokenInfo> tokenEntry) {
        long currentTime = System.currentTimeMillis();
        if (isTokenExpired(tokenEntry.getValue().expiresAt(), currentTime)) {
            blacklistedTokens.remove(tokenEntry.getKey(), tokenEntry.getValue());
            return false;
        }
        return true;
    }

    private record TokenInfo(String username, long expiresAt) {
    }

}