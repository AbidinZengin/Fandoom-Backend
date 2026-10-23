package com.example.fandoom_backend.user.repository;

import com.example.fandoom_backend.user.entity.Role;
import com.example.fandoom_backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByRole(Role role);
    Optional<User> findByEmailVerificationToken(String token);

    // pattern: çağıran tarafından '!' ile kaçırılmış LIKE deseni (ya da '%'). '\' yerine '!' seçildi: MySQL'de
    // ESCAPE '\' string literal'ini bozar.
    @Query("select u.id from User u where u.emailVerified = true and lower(u.username) like :pattern escape '!' "
            + "order by u.createdAt desc, u.id desc")
    Page<Long> findVerifiedIdsByUsernamePattern(@Param("pattern") String pattern, Pageable pageable);
}
