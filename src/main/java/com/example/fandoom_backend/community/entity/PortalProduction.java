package com.example.fandoom_backend.community.entity;

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

// Portal <-> yapım bağı. productionSlug movie/series modülüne gerçek FK değil, düz slug (cross-module
// ID-only kuralı); varlığı PortalServiceImpl'de MovieService/SeriesService.existsBySlug ile doğrulanır.
// uk_portal_production_slug: bir yapım EN FAZLA bir portala bağlıdır.
@Entity
@Table(name = "portal_production", uniqueConstraints = {
        @UniqueConstraint(name = "uk_portal_production_slug", columnNames = "production_slug"),
        @UniqueConstraint(name = "uk_portal_production_portal_slug", columnNames = {"portal_id", "production_slug"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class PortalProduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "portal_id", nullable = false)
    private Long portalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "production_type", nullable = false, length = 20)
    private PortalProductionType productionType;

    @Column(name = "production_slug", nullable = false, length = 280)
    private String productionSlug;
}
