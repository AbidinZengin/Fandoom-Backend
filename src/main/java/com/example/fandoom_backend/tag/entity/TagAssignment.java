package com.example.fandoom_backend.tag.entity;

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

@Entity
@Table(name = "tag_assignment", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tag_assignment",
                columnNames = {"tag_id", "taggable_type", "taggable_id"})
}, indexes = {
        @Index(name = "idx_tag_assignment_target", columnList = "taggable_type, taggable_id"),
        @Index(name = "idx_tag_assignment_tag", columnList = "tag_id, taggable_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class TagAssignment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tag_assignment_tag"))
    @ToString.Exclude
    private Tag tag;

    @Enumerated(EnumType.STRING)
    @Column(name = "taggable_type", nullable = false, length = 20)
    private TaggableType taggableType;

    // Cross-module referans: sadece ID, JPA ilişkisi YOK
    @Column(name = "taggable_id", nullable = false)
    private Long taggableId;
}
