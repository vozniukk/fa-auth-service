package org.example.auth.repository;

import org.example.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    /**
     * Найти активный refresh token по значению token
     */
    Optional<RefreshToken> findByTokenAndRevokedAtIsNull(String token);

    /**
     * Найти все refresh tokens для пользователя
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revokedAt IS NULL")
    java.util.List<RefreshToken> findActiveTokensByUserId(String userId);

    /**
     * Инвалидировать все refresh tokens пользователя
     */
    @Transactional
    @Modifying
    @Query("UPDATE RefreshToken SET revokedAt = CURRENT_TIMESTAMP WHERE userId = :userId AND revokedAt IS NULL")
    int revokeAllUserTokens(String userId);

    /**
     * Удалить истекшие refresh tokens
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM RefreshToken WHERE expiresAt < :expirationTime OR revokedAt < :revokedDaysAgo")
    int deleteExpiredTokens(LocalDateTime expirationTime, LocalDateTime revokedDaysAgo);
}

