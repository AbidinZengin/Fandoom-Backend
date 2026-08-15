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

// Yapıma özel, admin-tanımlı taksonomi kategorisi (ör. "Haneler", "Ejderha
// Türleri"). Eskiden Group.type sabit bir enum'du (FACTION/SPECIES); bu
// entity onun yerine geçti — yeni bir kategori adı artık kod değişikliği/
// deploy gerektirmeden admin panelinden eklenebilir. Kategori yapıma özeldir,
// global/ortak bir havuz DEĞİLDİR: iki farklı yapımda aynı isimli kategori
// olsa bile bağımsız kayıtlardır.
@Entity
@Table(name = "taxonomy_category", uniqueConstraints = {
        @UniqueConstraint(name = "uk_taxonomy_category_slug", columnNames = "slug")
}, indexes = {
        @Index(name = "idx_taxonomy_category_subject", columnList = "subject_type, subject_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class TaxonomyCategory extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "slug", nullable = false, length = 180)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 20)
    private SubjectType subjectType;

    // Cross-module referans: Movie ya da Series id'si, JPA ilişkisi YOK
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;
}
