package com.example.fandoom_backend.franchise.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "franchise", uniqueConstraints = {
        @UniqueConstraint(name = "uk_franchise_slug", columnNames = "slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class Franchise extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "slug", nullable = false, length = 180)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "banner_image_url", length = 500)
    private String bannerImageUrl;
}
