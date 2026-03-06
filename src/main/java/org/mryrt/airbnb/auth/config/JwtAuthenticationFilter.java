package org.mryrt.airbnb.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mryrt.airbnb.auth.exception.message.AuthErrorMessages;
import org.mryrt.airbnb.auth.service.jwt.DefaultJwtService;
import org.mryrt.airbnb.exception.ErrorResponse;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.mryrt.airbnb.auth.exception.message.AuthErrorMessages.DEAD_ACCOUNT;
import static org.mryrt.airbnb.auth.exception.message.AuthErrorMessages.EXPIRED_CREDENTIALS;
import static org.mryrt.airbnb.auth.exception.message.AuthErrorMessages.INVALID_CREDENTIALS;
import static org.mryrt.airbnb.auth.exception.message.AuthErrorMessages.UNEXPECTED_AUTH_EXCEPTION;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/login",
            "/auth/register",
            "/health/**"
    );

    private final DefaultJwtService jwtService;

    private final UserDetailsService userDetailsService;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final ObjectMapper objectMapper;

    @Override
    public void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws IOException {

        try {
            String jwt = extractJwtFromRequest(request);

            if (!StringUtils.hasText(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!jwtService.validateToken(jwt)) {
                handleAuthenticationError(response, INVALID_CREDENTIALS);
                return;
            }

            String username = jwtService.extractUsername(jwt);

            if (StringUtils.hasText(username) &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (!userDetails.isEnabled()) {
                    handleAuthenticationError(response, DEAD_ACCOUNT);
                    return;
                }

                if (!userDetails.isCredentialsNonExpired()) {
                    handleAuthenticationError(response, EXPIRED_CREDENTIALS);
                    return;
                }

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Authenticated user: {}", username);
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("JWT authentication failed for request: {}", request.getRequestURI());
            handleAuthenticationError(response, UNEXPECTED_AUTH_EXCEPTION);
        }
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring("Bearer ".length());
        }

        return null;
    }

    private void handleAuthenticationError(HttpServletResponse response, AuthErrorMessages message, Object... args) throws IOException {
        SecurityContextHolder.clearContext();

        response.setStatus(message.getHttpStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(message.getHttpStatus().value())
                .message(message.getFormattedMessage(args))
                .timestamp(LocalDateTime.now())
                .build();

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return (HttpMethod.OPTIONS.matches(request.getMethod())
                || PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, request.getServletPath())));
    }

}