package com.example.fandoom_backend.community.entity;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// subjectType/subjectId: polimorfik cross-module referans (person/Cast'teki
// subjectType+subjectId deseninin aynısı) — THREAD/BLOG/SEASON/EPISODE'a
// gerçek FK yok, doğrulama CommentServiceImpl.validateSubject'te servis
// interface'leri üzerinden yapılır. parent: aggregate-içi gerçek JPA ilişkisi
// (cross-module DEĞİL, Comment zaten community/ modülünün kendi entity'si).
// parent==null -> üst seviye yorum, dolu -> yanıt. Kendi parent'ı dolu olan
// bir Comment'e yanıt verilemez (2 seviye sabit) — bu kısıt DB'de değil
// CommentServiceImpl.createForSubject'te uygulanır. Hard-delete/cascade
// kasıtlı olarak YOK: silme = status=DELETED (bkz. ThreadStatus.DELETED
// yorumu), body mapper'da "[silindi]" olarak maskelenir.
@Entity
@Table(name = "comment", indexes = {
        // Üst seviye yorum listesi: WHERE subject_type=? AND subject_id=? AND parent_id IS NULL
        // ORDER BY created_at|like_count DESC. Eski indeksteki araya giren "status" sütunu
        // ORDER BY'ı bozuyor (filesort) ve sorguda status filtresi yoktu; parent_id öne alındı.
        @Index(name = "idx_comment_subject_parent_created", columnList = "subject_type, subject_id, parent_id, created_at DESC"),
        @Index(name = "idx_comment_subject_parent_likes", columnList = "subject_type, subject_id, parent_id, like_count DESC"),
        // Yanıt önizlemesi: WHERE parent_id IN (...) ORDER BY created_at (pencere fonksiyonu PARTITION/ORDER)
        @Index(name = "idx_comment_parent_created", columnList = "parent_id, created_at"),
        // countByAuthorIdAndStatus (profil sayaçları)
        @Index(name = "idx_comment_author_status", columnList = "author_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class Comment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 20)
    private CommentSubjectType subjectType;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_comment_parent"))
    @ToString.Exclude
    private Comment parent;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(name = "spoiler_flagged", nullable = false)
    @Builder.Default
    private boolean spoilerFlagged = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CommentStatus status = CommentStatus.PUBLISHED;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private int likeCount = 0;
}
