package org.example.auth.repository;

import org.example.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    /**
     * Найти пользователя по email
     */
    Optional<User> findByEmail(String email);

    /**
     * Найти пользователя по Google ID
     */
    Optional<User> findByGoogleId(String googleId);

    /**
     * Проверить существует ли пользователь
     */
    boolean existsByEmail(String email);
}

