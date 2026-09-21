package com.example.fandoom_backend.community.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// ThreadLike ile aynı desen (surrogate id + unique constraint). joinedAt = createdAt'ten ayrı, açık
// alan ("Portallarım" joinedAt azalan sıralanır). Join/leave/me akışı B3 kapsamında; entity şimdiden var.
@Entity
@Table(name = "portal_membership", uniqueConstraints = {
        @UniqueConstraint(name = "uk_portal_membership", columnNames = {"portal_id", "user_id"})
}, indexes = {
        @Index(name = "idx_portal_membership_user", columnList = "user_id, joined_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class PortalMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "portal_id", nullable = false)
    private Long portalId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;
}
