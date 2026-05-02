package com.linkforge.urlshortener.repository;

import com.linkforge.urlshortener.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// Repository for User entity database operations
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by username (used for login)
    Optional<User> findByUsername(String username);

    // Find user by email (used for duplicate check)
    Optional<User> findByEmail(String email);

    // Check if a username is already taken
    boolean existsByUsername(String username);

    // Check if an email is already registered
    boolean existsByEmail(String email);
}
