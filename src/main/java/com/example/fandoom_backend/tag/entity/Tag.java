package com.example.fandoom_backend.tag.entity;

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

@Entity
@Table(name = "tag", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tag_name", columnNames = "name"),
        @UniqueConstraint(name = "uk_tag_slug", columnNames = "slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class Tag extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "slug", nullable = false, length = 80)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Blog hub facet filtreleri için (FORMAT/MOOD/THEME). Movie/Series/Person/
    // Character tag'lerinde null kalır, geriye dönük uyumluluk bu şekilde korunur.
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private TagType type;
}
