package com.example.fandoom_backend.blog.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

// Gövde, sabit body/quote kolonları yerine sıralı, tipli blok listesidir:
// bir blog 3 paragraf + 1 quote olabilir, başkası paragraf+resim+paragraf.
@Entity
@Table(name = "blog_block", indexes = {
        @Index(name = "idx_blog_block_blog", columnList = "blog_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class BlogBlock extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_blog_block_blog"))
    @ToString.Exclude
    private Blog blog;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 20)
    private BlogBlockType blockType;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "image_alt", length = 255)
    private String imageAlt;
}
