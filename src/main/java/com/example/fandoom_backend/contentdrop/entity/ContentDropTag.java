package com.example.fandoom_backend.contentdrop.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

// "Dive Deeper" carousel'inin (bölüm sayfası -> content drop) algoritmik
// kademeli-geri-düşüş kaynağı. Cast'teki subjectType+subjectId polimorfik
// desenin genişletilmişi: bir satır ya production-eksenli (subjectType+
// subjectId, opsiyonel seasonNumber/episodeNumber) ya da evren-eksenli
// (yalnızca franchiseId) olur — ikisi karşılıklı dışlayıcıdır, servis
// katmanında doğrulanır.
@Entity
@Table(name = "content_drop_tag", indexes = {
        @Index(name = "idx_content_drop_tag_content_drop", columnList = "content_drop_id"),
        @Index(name = "idx_content_drop_tag_subject", columnList = "subject_type, subject_id"),
        @Index(name = "idx_content_drop_tag_franchise", columnList = "franchise_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class ContentDropTag extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_drop_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_content_drop_tag_content_drop"))
    @ToString.Exclude
    private ContentDrop contentDrop;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", length = 20)
    private SubjectType subjectType;

    // Cross-module referans: Movie ya da Series id'si, JPA ilişkisi YOK
    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "season_number")
    private Integer seasonNumber;

    @Column(name = "episode_number")
    private Integer episodeNumber;

    // Cross-module referans: franchise/ modülüne, JPA ilişkisi YOK
    @Column(name = "franchise_id")
    private Long franchiseId;
}
