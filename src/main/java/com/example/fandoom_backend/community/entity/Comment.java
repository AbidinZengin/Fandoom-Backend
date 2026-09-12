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

// thread/parent: aggregate-içi gerçek JPA ilişkisi (cross-module DEĞİL,
// Comment zaten community/ modülünün kendi entity'si). parent==null -> üst
// seviye yorum, dolu -> yanıt. Kendi parent'ı dolu olan bir Comment'e yanıt
// verilemez (2 seviye sabit) — bu kısıt DB'de değil CommentServiceImpl.create'de
// uygulanır. Hard-delete/cascade kasıtlı olarak YOK: silme = status=DELETED
// (bkz. ThreadStatus.DELETED yorumu), body mapper'da "[silindi]" olarak maskelenir.
@Entity
@Table(name = "comment", indexes = {
        @Index(name = "idx_comment_thread_status_created", columnList = "thread_id, status, created_at DESC"),
        @Index(name = "idx_comment_parent", columnList = "parent_id"),
        @Index(name = "idx_comment_author", columnList = "author_id")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_comment_thread"))
    @ToString.Exclude
    private Thread thread;

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
