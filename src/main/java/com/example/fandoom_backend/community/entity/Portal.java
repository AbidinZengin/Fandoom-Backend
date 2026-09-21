package com.example.fandoom_backend.community.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Community "odası": her Thread mecburen tek bir portala aittir (Thread.portalId). Slug DEĞİŞMEZ
// (FE URL'i /portal/{slug}/...). memberCount/threadCount denormalize sayaçlardır — PortalRepository'deki
// native @Modifying sorgularla güncellenir, entity'nin in-memory alanı bilerek set edilmez (Thread
// sayaçlarıyla aynı gerekçe: dirty-checking bulk update'i stale değerle ezmesin). threadCount yalnız
// PUBLISHED thread'leri sayar.
@Entity
@Table(name = "portal", uniqueConstraints = {
        @UniqueConstraint(name = "uk_portal_slug", columnNames = "slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class Portal extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 50)
    private String slug;

    @Column(name = "name_tr", nullable = false, length = 60)
    private String nameTr;

    @Column(name = "name_en", nullable = false, length = 60)
    private String nameEn;

    @Column(name = "description_tr", length = 300)
    private String descriptionTr;

    @Column(name = "description_en", length = 300)
    private String descriptionEn;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    // Opsiyonel vurgu rengi (#RRGGBB). Backend yalnız saklar; gradyan/okunabilirlik FE'de kurulur.
    @Column(name = "accent_color", length = 7)
    private String accentColor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PortalStatus status = PortalStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "posting_policy", nullable = false, length = 20)
    @Builder.Default
    private PortalPostingPolicy postingPolicy = PortalPostingPolicy.OPEN;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(name = "member_count", nullable = false)
    @Builder.Default
    private int memberCount = 0;

    @Column(name = "thread_count", nullable = false)
    @Builder.Default
    private int threadCount = 0;
}
