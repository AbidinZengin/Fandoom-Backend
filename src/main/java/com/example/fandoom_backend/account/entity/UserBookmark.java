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

// "Kaydet" (Save) — UserSavedItem'dan (Watchlist/Readlist/Watched/Custom liste
// üyeliği) KASITLI olarak ayrı: liste kavramına hiç bağlı değil, UserLike ile
// birebir aynı desen (flat, idempotent toggle). itemId/itemType: cross-module
// referans, sadece ID.
@Entity
@Table(name = "user_bookmark", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_bookmark", columnNames = {"user_id", "item_id", "item_type"})
}, indexes = {
        @Index(name = "idx_user_bookmark_item", columnList = "item_type, item_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class UserBookmark extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private SavedItemType itemType;
}
