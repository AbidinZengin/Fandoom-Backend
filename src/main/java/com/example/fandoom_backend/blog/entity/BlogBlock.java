package com.example.fandoom_backend.blog.entity;

import com.example.fandoom_backend.content.entity.ContentBlock;
import jakarta.persistence.*;
import lombok.*;

// Gövde, sabit body/quote kolonları yerine sıralı, tipli blok listesidir:
// bir blog 3 paragraf + 1 quote olabilir, başkası paragraf+resim+paragraf.
// id/orderIndex ContentBlock'tan miras (bkz. content/entity/ContentBlock).
// ContentBlock'un col/row'u (CSS grid) blog için artık KULLANILMIYOR —
// serbest kanvas konumlama (x/y/width/height) bu sınıfa özel alanlar olarak
// tutulur (HomeBlock/EpisodeBlock hâlâ col/row'a dayanıyor, bu yüzden
// ContentBlock'tan taşınmadı). x/y/width, DB'de bilerek nullable — mevcut
// blog_block satırları (eski col/row döneminden) bu alanlar olmadan var,
// NOT NULL yapmak Hibernate şema migration'ını kırar (bkz. episode_block
// enum truncation hatası); "zorunlu" kuralı yalnızca BlogBlockRequest'teki
// @NotNull ile API sınırında uygulanır.
// @Builder yalnızca burada tanımlı alanları kapsar — orderIndex
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

    @Column(name = "text_tr", columnDefinition = "TEXT")
    private String textTr;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "image_alt_tr", length = 255)
    private String imageAltTr;

    @Column(name = "image_alt", length = 255)
    private String imageAlt;

    private Double x;

    private Double y;

    private Double width;

    private Double height;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BlockAnimation animation;

    @Column(name = "font_family", length = 20)
    @Enumerated(EnumType.STRING)
    private BlockFontFamily fontFamily;

    @Column(name = "font_scale")
    @Builder.Default
    private Double fontScale = 1.0;
}
