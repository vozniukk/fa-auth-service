package org.example.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.example.auth.dto.RefreshTokenRequest;
import org.example.auth.entity.RefreshToken;
import org.example.auth.entity.User;
import org.example.auth.repository.RefreshTokenRepository;
import org.example.auth.repository.UserRepository;
import org.example.shared.dto.LoginRequest;
import org.example.shared.dto.TokenResponse;
import org.example.shared.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Auth Service - генерация JWT токенов и управление refresh tokens
 */
@Slf4j
@Service
public class AuthService {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    /**
     * Обработать Google OAuth2 callback
     */
    public TokenResponse handleGoogleCallback(LoginRequest request) {
        log.info("Processing Google OAuth2 callback");

        // В production нужно обменять code на Google token
        // Здесь симуляция - используем code как идентификатор для демонстрации
        String googleId = UUID.randomUUID().toString();
        String email = "user-" + System.currentTimeMillis() + "@gmail.com"; // Симуляция email

        // Попытаться найти существующего пользователя
        User user = userRepository.findByGoogleId(googleId)
                .orElse(null);

        // Если пользователь не существует - создать с ролью GUEST
        if (user == null) {
            user = User.builder()
                    .email(email)
                    .googleId(googleId)
                    .fullName("User " + System.currentTimeMillis())
                    .profilePictureUrl(null)
                    .isActive(true)
                    .build();
            userRepository.save(user);
            log.info("Created new user: {}", email);
        }

        // Генерировать токены (всегда с ролью GUEST для новых пользователей)
        List<String> roles = Arrays.asList("GUEST");
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // Сохранить refresh token в БД
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .id(UUID.randomUUID().toString())
                .userId(user.getEmail())
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        log.info("Generated tokens for user: {}", user.getEmail());

        return TokenResponse.builder()
                .access_token(accessToken)
                .refresh_token(refreshToken)
                .token_type("Bearer")
                .expires_in(900)  // 15 минут в секундах
                .user_email(user.getEmail())
                .roles(roles)
                .build();
    }

    /**
     * Обновить access token используя refresh token
     */
    public TokenResponse refreshAccessToken(RefreshTokenRequest request) {
        log.info("Refreshing access token");

        // Найти refresh token в БД
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenAndRevokedAtIsNull(request.getRefresh_token())
                .orElseThrow(() -> {
                    log.warn("Invalid or revoked refresh token");
                    return new RuntimeException("Invalid refresh token");
                });

        // Проверить что токен не истек
        if (refreshToken.isExpired()) {
            log.warn("Refresh token expired");
            throw new RuntimeException("Refresh token expired");
        }

        // Получить email пользователя
        String userEmail = refreshToken.getUserId();

        // Получить роли пользователя (в production получить из User Service)
        List<String> roles = Arrays.asList("GUEST");  // Заглушка

        // Генерировать новый access token
        String newAccessToken = jwtTokenProvider.generateAccessToken(userEmail, roles);

        log.info("Generated new access token for user: {}", userEmail);

        return TokenResponse.builder()
                .access_token(newAccessToken)
                .token_type("Bearer")
                .expires_in(900)
                .refresh_token_expires_in(604800)
                .build();
    }

    /**
     * Logout - инвалидировать все refresh tokens пользователя
     */
    public void logout(String email) {
        log.info("Logging out user: {}", email);

        int revokedCount = refreshTokenRepository.revokeAllUserTokens(email);
        log.info("Revoked {} refresh tokens for user: {}", revokedCount, email);
    }

    /**
     * Cleanup - удалить истекшие токены (запускается периодически)
     */
    public void cleanupExpiredTokens() {
        log.info("Cleaning up expired refresh tokens");

        int deletedCount = refreshTokenRepository.deleteExpiredTokens(
                LocalDateTime.now(),
                LocalDateTime.now().minusDays(30)
        );
        log.info("Deleted {} expired refresh tokens", deletedCount);
    }
}

