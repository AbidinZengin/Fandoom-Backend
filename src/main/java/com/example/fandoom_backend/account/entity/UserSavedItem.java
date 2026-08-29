package com.example.fandoom_backend.account.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// itemId/itemType: cross-module referans, sadece ID (Movie/Series/Blog'a JPA
// ilişkisi YOK) — varlığı UserSavedItemServiceImpl'de MovieService/
// SeriesService/BlogService.existsById ile doğrulanır (Cast'in subjectType/
// subjectId deseni). userList: aggregate-içi gerçek @ManyToOne (account/
// modülünün kendi içinde, Blog.blocks ile aynı desende).
@Entity
@Table(name = "user_saved_item", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_saved_item",
                columnNames = {"user_id", "item_id", "item_type", "user_list_id"})
}, indexes = {
        @Index(name = "idx_user_saved_item_user_list_id", columnList = "user_list_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class UserSavedItem extends Auditable {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_list_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_saved_item_user_list"))
    @ToString.Exclude
    private UserList userList;

    // Sadece BLOG için anlamlı (okuma ilerlemesi); MOVIE/SERIES'te bilgi
    // amaçlı boş kalır, servis katmanında zorlanmaz.
    @Column(name = "progress_percentage")
    private Integer progressPercentage;

    @Column(name = "notes", length = 500)
    private String notes;
}
