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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// userId: cross-module referans, düz Long. listType != CUSTOM olan satırlar
// (WATCHLIST/READLIST) kullanıcı başına lazy oluşturulan, silinemeyen/tipi
// değiştirilemeyen sistem listeleridir (bkz. UserListServiceImpl).
@Entity
@Table(name = "user_list", indexes = {
        @Index(name = "idx_user_list_user_id", columnList = "user_id"),
        @Index(name = "idx_user_list_user_id_list_type", columnList = "user_id, list_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class UserList extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "is_pinned", nullable = false)
    private boolean isPinned;

    @Enumerated(EnumType.STRING)
    @Column(name = "list_type", nullable = false, length = 20)
    private ListType listType;
}
