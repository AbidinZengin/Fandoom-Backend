package com.example.fandoom_backend.user.repository;

import com.example.fandoom_backend.user.entity.Role;
import com.example.fandoom_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByRole(Role role);
    Optional<User> findByEmailVerificationToken(String token);
}
