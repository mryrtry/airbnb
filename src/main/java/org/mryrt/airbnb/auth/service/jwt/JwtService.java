package org.mryrt.airbnb.auth.service.jwt;

import java.util.Map;

public interface JwtService {

    Map<String, String> generateTokenPair(String username);

    Map<String, String> refreshAccessToken(String refreshToken);

    String extractUsername(String token);

    boolean validateToken(String token);

    void invalidateToken(String token, String username);

}
