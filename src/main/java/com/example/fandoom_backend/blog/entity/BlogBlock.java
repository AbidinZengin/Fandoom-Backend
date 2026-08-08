package com.example.fandoom_backend.blog.entity;

import com.example.fandoom_backend.content.entity.ContentBlock;
import jakarta.persistence.*;
import lombok.*;

// Gövde, sabit body/quote kolonları yerine sıralı, tipli blok listesidir:
// bir blog 3 paragraf + 1 quote olabilir, başkası paragraf+resim+paragraf.
// id/orderIndex/col/row ContentBlock'tan miras (bkz. content/entity/ContentBlock).
// @Builder yalnızca burada tanımlı alanları kapsar — orderIndex/col/row
// build() sonrası setter ile atanır (bkz. BlogServiceImpl.applyBlocks).
@Entity
@Table(name = "blog_block", indexes = {
         @Index(name = "idx_blog_block_blog", columnList = "blog_id")
})
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString
public class BlogBlock extends ContentBlock {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_blog_block_blog"))
    @ToString.Exclude
    private Blog blog;

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
