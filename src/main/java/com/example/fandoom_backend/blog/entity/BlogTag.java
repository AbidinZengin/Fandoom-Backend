package com.example.fandoom_backend.blog.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

// "Dive Deeper" carousel'inin (bölüm sayfası -> blog) algoritmik
// kademeli-geri-düşüş kaynağı. Cast'teki subjectType+subjectId polimorfik
// desenin genişletilmişi: bir satır ya production-eksenli (subjectType+
// subjectId, opsiyonel seasonNumber/episodeNumber) ya da evren-eksenli
// (yalnızca franchiseId) olur — ikisi karşılıklı dışlayıcıdır, servis
// katmanında doğrulanır.
@Entity
@Table(name = "blog_tag", indexes = {
        @Index(name = "idx_blog_tag_blog", columnList = "blog_id"),
        @Index(name = "idx_blog_tag_subject", columnList = "subject_type, subject_id"),
        @Index(name = "idx_blog_tag_franchise", columnList = "franchise_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class BlogTag extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_blog_tag_blog"))
    @ToString.Exclude
    private Blog blog;

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
