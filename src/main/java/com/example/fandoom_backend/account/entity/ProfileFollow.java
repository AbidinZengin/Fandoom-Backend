package com.example.fandoom_backend.account.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.DynamicUpdate;

// Kişi takibi (kullanıcı -> kullanıcı). MOVIE/SERIES/BLOG takibi olan UserFollow'dan bilinçli ayrı:
// bu ilişki bir durum taşır (FOLLOWING/REQUESTED). followerId/followedId user/'a düz Long (FK yok).
@Entity
@Table(name = "profile_follow", uniqueConstraints = {
        @UniqueConstraint(name = "uk_profile_follow", columnNames = {"follower_id", "followed_id"})
}, indexes = {
        @Index(name = "idx_profile_follow_followed", columnList = "followed_id, status, created_at"),
        @Index(name = "idx_profile_follow_follower", columnList = "follower_id, status, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
@DynamicUpdate
public class ProfileFollow extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "followed_id", nullable = false)
    private Long followedId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FollowState status;
}
