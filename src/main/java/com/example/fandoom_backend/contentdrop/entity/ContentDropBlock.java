package com.example.fandoom_backend.contentdrop.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

// Gövde, sabit body/quote kolonları yerine sıralı, tipli blok listesidir:
// bir content drop 3 paragraf + 1 quote olabilir, başkası paragraf+resim+paragraf.
@Entity
@Table(name = "content_drop_block", indexes = {
        @Index(name = "idx_content_drop_block_content_drop", columnList = "content_drop_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class ContentDropBlock extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_drop_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_content_drop_block_content_drop"))
    @ToString.Exclude
    private ContentDrop contentDrop;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 20)
    private ContentDropBlockType blockType;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "image_alt", length = 255)
    private String imageAlt;
}
