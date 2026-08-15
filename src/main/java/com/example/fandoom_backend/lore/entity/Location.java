package com.example.fandoom_backend.lore.entity;

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

// Bir yapımın "yer" kavramı (ör. "Winterfell"). Bilinçli olarak hafif
// tutuldu: hiyerarşi (kıta/bölge/şehir zinciri) YOK — YAGNI. Sunuma özgü
// alanlar (harita koordinatı, ölçek vb.) çekirdek kolon DEĞİL — her yapımın
// "yer"i farklı şekilde sunabilir (2D harita, liste, başka bir şey), bu
// yüzden customFields'a gider (Group.customFields'la aynı felsefe: backend
// içeriği doğrulamaz/yorumlamaz, opak saklar). Bir yerin hangi haneye/gruba
// ait olduğu customFields'a DEĞİL, mevcut GroupAssignment mekanizmasına
// (TaggableType.LOCATION) gider — gerçek bir ilişki, opak veri değil.
@Entity
@Table(name = "location", uniqueConstraints = {
        @UniqueConstraint(name = "uk_location_slug", columnNames = "slug")
}, indexes = {
        @Index(name = "idx_location_subject", columnList = "subject_type, subject_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class Location extends Auditable {

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

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Sunuma özgü, yapıma özel alanlar (ör. harita x/y koordinatı, ölçek) —
    // backend içeriği doğrulamaz/yorumlamaz, opak JSON blob olarak saklar.
    @Column(name = "custom_fields", columnDefinition = "json")
    private String customFields;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 20)
    private SubjectType subjectType;

    // Cross-module referans: Movie ya da Series id'si, JPA ilişkisi YOK
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;
}
