package com.example.fandoom_backend.lore.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

// Bir taksonomi kategorisinin somut değeri (ör. "House Stark", kategorisi
// "Haneler"). Hangi yapıma ait olduğu artık kendi üzerinde tutulmuyor —
// category (aynı modül içi gerçek FK) üzerinden türetiliyor, tek doğruluk
// kaynağı korunuyor (eskiden Group'un kendi subjectType/subjectId'si vardı,
// TaxonomyCategory eklenince redundant hale geldi ve kaldırıldı).
@Entity
@Table(name = "content_group", uniqueConstraints = {
        @UniqueConstraint(name = "uk_group_slug", columnNames = "slug")
}, indexes = {
        @Index(name = "idx_group_category", columnList = "category_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class Group extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "slug", nullable = false, length = 180)
    private String slug;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_group_category"))
    @ToString.Exclude
    private TaxonomyCategory category;

    // Kategoriye özel serbest alanlar (ör. Haneler için "sigil"/"words",
    // Ejderhalar için "power") — backend içeriği doğrulamaz/yorumlamaz,
    // opak JSON blob olarak saklar (ContentBlock.col/row felsefesiyle aynı).
    @Column(name = "custom_fields", columnDefinition = "json")
    private String customFields;
}
