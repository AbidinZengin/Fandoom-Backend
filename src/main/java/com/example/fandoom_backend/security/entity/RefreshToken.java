package com.example.fandoom_backend.security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// userId düz Long (FK yok): security/ modülü user/'dan kasıtlı ayrı (bkz. RevokedToken/CLAUDE.md).
@Entity
@Table(name = "refresh_token", uniqueConstraints = {
        @UniqueConstraint(name = "uk_refresh_token_token", columnNames = "token")
}, indexes = {
        @Index(name = "idx_refresh_token_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "token", nullable = false, length = 36)
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Builder.Default
    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;
}
