package org.mryrt.airbnb.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mryrt.airbnb.auth.dto.UserDto;
import org.mryrt.airbnb.auth.dto.request.LoginRequest;
import org.mryrt.airbnb.auth.dto.request.TokenRequest;
import org.mryrt.airbnb.auth.dto.request.UserRequest;
import org.mryrt.airbnb.auth.service.jwt.DefaultJwtService;
import org.mryrt.airbnb.auth.service.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * API аутентификации и работы с JWT (регистрация, вход, обновление и проверка токена, выход).
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    private final DefaultJwtService jwtService;

    /** Регистрация нового пользователя и выдача пары токенов. */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody UserRequest request) {
        log.info("Registering new user: {}", request.getUsername());
        UserDto user = userService.create(request);
        Map<String, String> tokens = jwtService.generateTokenPair(user.getUsername());
        request.clearPassword();
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "user", user,
                "tokens", tokens
        ));
    }

    /** Вход по логину и паролю, выдача пары токенов. */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        userService.validateLogin(request);
        UserDto user = userService.get(request.getUsername());
        Map<String, String> tokens = jwtService.generateTokenPair(request.getUsername());
        log.info("User {} successfully authenticated", request.getUsername());
        request.clearPassword();
        return ResponseEntity.ok(Map.of(
                "user", user,
                "tokens", tokens
        ));
    }

    /** Обновление access-токена по refresh-токену. */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@Valid @RequestBody TokenRequest request) {
        log.info("Refreshing token");
        Map<String, String> tokens = jwtService.refreshAccessToken(request.getToken());
        return ResponseEntity.ok(tokens);
    }

    /** Выход: инвалидация токена и очистка контекста. */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null) ? auth.getName() : "unknown";
        if (token != null) {
            jwtService.invalidateToken(token, username);
        }
        SecurityContextHolder.clearContext();
        log.info("User {} logged out", username);
        return ResponseEntity.ok().build();
    }

    /** Проверка валидности токена и возврат имени пользователя при успехе. */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@Valid @RequestBody TokenRequest request) {
        boolean isValid = jwtService.validateToken(request.getToken());
        String username = isValid ? jwtService.extractUsername(request.getToken()) : null;
        return ResponseEntity.ok(Map.of(
                "valid", isValid,
                "username", username != null ? username : ""
        ));
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring("Bearer ".length());
        }
        return null;
    }

}