package com.example.fandoom_backend.group.entity;

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
@Table(name = "group_assignment", uniqueConstraints = {
        @UniqueConstraint(name = "uk_group_assignment",
                columnNames = {"group_id", "taggable_type", "taggable_id"})
}, indexes = {
        @Index(name = "idx_group_assignment_target", columnList = "taggable_type, taggable_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class GroupAssignment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_group_assignment_group"))
    @ToString.Exclude
    private Group group;

    @Enumerated(EnumType.STRING)
    @Column(name = "taggable_type", nullable = false, length = 20)
    private TaggableType taggableType;

    // Cross-module referans: sadece ID, JPA ilişkisi YOK
    @Column(name = "taggable_id", nullable = false)
    private Long taggableId;
}
