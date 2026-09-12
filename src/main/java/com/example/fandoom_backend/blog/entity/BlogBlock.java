package com.example.fandoom_backend.blog.entity;

import com.example.fandoom_backend.content.entity.ContentBlock;
import jakarta.persistence.*;
import lombok.*;

// SeasonBlock ile BİREBİR aynı desen (kullanıcı kararı, 2026-08): gövde
// artık serbest x/y/width/height canvas DEĞİL, sceneKey'e göre gruplanan
// sıralı sahne blokları (bkz. series/entity/SeasonBlock). id/orderIndex
// ContentBlock'tan miras; col/row bu sınıfta hiç kullanılmaz (Season'da da
// kullanılmıyor).
// KASITLI FARKLAR (kullanıcı kararı): blockType SeasonBlockType/EpisodeBlockType
// gibi MEDIA değil IMAGE kalır (blog'un görsel bloğu her zaman düz bir
// fotoğraftır, video/başka medya türü yok — MEDIA fazla genel kaçardı);
// alanlar da buna uygun imageUrl/imageAlt(Tr) — mediaUrl değil. Season'ın
// mediaCredit'i de YOK — blog'da fotoğraf kredisi gösterme ihtiyacı yok.
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

    @Column(name = "scene_key", nullable = false, length = 100)
    private String sceneKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 20)
    private BlogBlockType blockType;

    // IMAGE'de null.
    @Column(name = "content_tr", columnDefinition = "TEXT")
    private String contentTr;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    // Yalnızca IMAGE'de dolu.
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "image_alt_tr", length = 255)
    private String imageAltTr;

    @Column(name = "image_alt", length = 255)
    private String imageAlt;
}
