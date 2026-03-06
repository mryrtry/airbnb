package org.mryrt.airbnb.auth.service.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mryrt.airbnb.auth.exception.message.AuthErrorMessages;
import org.mryrt.airbnb.auth.service.jwt.blacklist.BlacklistService;
import org.mryrt.airbnb.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultJwtService implements JwtService {

    private final BlacklistService tokenBlacklistService;

    @Value("${airbnb.jwt.secret}")
    private String jwtSecret;

    @Value("${airbnb.jwt.access-token.expiration:15m}")
    private Duration accessTokenExpiration;

    @Value("${airbnb.jwt.refresh-token.expiration:7d}")
    private Duration refreshTokenExpiration;

    private String createToken(String username, Duration expiration) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(expiration);

        return Jwts.builder()
                .setClaims(new HashMap<>())
                .setSubject(username)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiryDate))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private String generateAccessToken(String username) {
        return createToken(username, accessTokenExpiration);
    }

    private String generateRefreshToken(String username) {
        return createToken(username, refreshTokenExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) throws MalformedJwtException {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) throws MalformedJwtException {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Date extractExpiration(String token) throws MalformedJwtException {
        return extractClaim(token, Claims::getExpiration);
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private boolean isTokenAlive(String token) {
        return !extractExpiration(token).before(new Date());
    }

    @Override
    public Map<String, String> generateTokenPair(String username) {
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", generateAccessToken(username));
        tokens.put("refreshToken", generateRefreshToken(username));
        return tokens;
    }

    @Override
    public Map<String, String> refreshAccessToken(String refreshToken) {
        if (!validateToken(refreshToken)) {
            throw new ServiceException(AuthErrorMessages.INVALID_CREDENTIALS, "refresh");
        }
        if (tokenBlacklistService.isTokenBlacklisted(refreshToken)) {
            throw new ServiceException(AuthErrorMessages.TOKEN_BLACKLISTED, "refresh");
        }
        String username = extractUsername(refreshToken);
        return generateTokenPair(username);
    }

    @Override
    public String extractUsername(String token) throws MalformedJwtException {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            if (tokenBlacklistService.isTokenBlacklisted(token)) {
                log.warn("Attempt to use blacklisted token");
                return false;
            }
            return isTokenAlive(token);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void invalidateToken(String token, String username) {
        Instant expiresAt = extractExpiration(token).toInstant();
        tokenBlacklistService.blacklistToken(token, username, expiresAt);
        log.info("Token invalidated for user: {}", username);
    }

}