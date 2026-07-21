package com.example.fandoom_backend.person.entity;

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
@Table(name = "cast_member", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cast_subject_person_character",
                columnNames = {"subject_type", "subject_id", "person_id", "character_id"})
}, indexes = {
        @Index(name = "idx_cast_subject", columnList = "subject_type, subject_id"),
        @Index(name = "idx_cast_person_id", columnList = "person_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class Cast extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cast_person"))
    @ToString.Exclude
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cast_character"))
    @ToString.Exclude
    private Character character;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 20)
    private SubjectType subjectType;

    // Cross-module referans: Movie ya da Series id'si, JPA ilişkisi YOK
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "billing_order")
    private Integer billingOrder;
}
