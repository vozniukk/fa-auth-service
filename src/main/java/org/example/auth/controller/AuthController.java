package org.example.auth.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.auth.dto.RefreshTokenRequest;
import org.example.auth.service.AuthService;
import org.example.shared.dto.ErrorResponse;
import org.example.shared.dto.LoginRequest;
import org.example.shared.dto.TokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST контроллер для аутентификации
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * POST /auth/google/callback
     * Callback endpoint для Google OAuth2
     */
    @PostMapping("/google/callback")
    public ResponseEntity<?> googleCallback(@RequestBody LoginRequest request) {
        try {
            log.info("Received Google OAuth2 callback");

            if (request.getCode() == null || request.getCode().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ErrorResponse.builder()
                                .error("invalid_request")
                                .error_description("Authorization code is required")
                                .status(400)
                                .timestamp(LocalDateTime.now())
                                .build()
                );
            }

            TokenResponse tokenResponse = authService.handleGoogleCallback(request);
            return ResponseEntity.ok(tokenResponse);

        } catch (Exception e) {
            log.error("Error processing Google callback", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ErrorResponse.builder()
                            .error("unauthorized")
                            .error_description("Google OAuth2 authentication failed: " + e.getMessage())
                            .status(401)
                            .timestamp(LocalDateTime.now())
                            .build()
            );
        }
    }

    /**
     * POST /auth/refresh
     * Обновить access token используя refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            log.info("Processing token refresh request");

            if (request.getRefresh_token() == null || request.getRefresh_token().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ErrorResponse.builder()
                                .error("bad_request")
                                .error_description("Refresh token is required")
                                .status(400)
                                .timestamp(LocalDateTime.now())
                                .build()
                );
            }

            TokenResponse tokenResponse = authService.refreshAccessToken(request);
            return ResponseEntity.ok(tokenResponse);

        } catch (RuntimeException e) {
            log.error("Error refreshing token", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ErrorResponse.builder()
                            .error("invalid_token")
                            .error_description(e.getMessage())
                            .status(401)
                            .timestamp(LocalDateTime.now())
                            .build()
            );
        }
    }

    /**
     * POST /auth/logout
     * Logout и инвалидировать refresh tokens
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        ErrorResponse.builder()
                                .error("unauthorized")
                                .error_description("User is not authenticated")
                                .status(401)
                                .timestamp(LocalDateTime.now())
                                .build()
                );
            }

            String userEmail = authentication.getName();
            log.info("Logging out user: {}", userEmail);

            authService.logout(userEmail);

            return ResponseEntity.ok(Map.of(
                    "message", "Logged out successfully",
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error logging out", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse.builder()
                            .error("internal_error")
                            .error_description("Failed to process logout")
                            .status(500)
                            .timestamp(LocalDateTime.now())
                            .build()
            );
        }
    }

    /**
     * GET /auth/health
     * Простая проверка здоровья endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "OK"));
    }
}

import java.util.Map;

