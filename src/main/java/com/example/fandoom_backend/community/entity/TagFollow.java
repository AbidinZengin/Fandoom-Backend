package com.example.fandoom_backend.community.entity;

import com.example.fandoom_backend.common.entity.Auditable;
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

// ThreadLike/ThreadBookmark ile aynı desen (surrogate id + unique constraint,
// composite PK değil) — ama bilinçli olarak BAĞIMSIZ: account/'daki genel
// SavedItem/UserFollow mekanizması (SavedItemType) burada kasıtlı olarak
// kullanılmıyor. Community'de kullanıcı serbestçe tag yazabiliyor (ThreadTag
// gibi), bu account/'ın rol/tip modeliyle uyuşmuyor. tag alanı
// SlugGenerator.slugify ile normalize edilir (TagFollowServiceImpl).
@Entity
@Table(name = "tag_follow", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tag_follow", columnNames = {"user_id", "tag"})
}, indexes = {
        @Index(name = "idx_tag_follow_tag", columnList = "tag")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class TagFollow extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String tag;
}
