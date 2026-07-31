package com.example.fandoom_backend.contentdrop.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "content_drop", uniqueConstraints = {
        @UniqueConstraint(name = "uk_content_drop_slug", columnNames = "slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class ContentDrop extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 280)
    private String slug;

    @Column(length = 255)
    private String kicker;

    @Column(length = 255)
    private String axis;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "image_url_large", length = 500)
    private String imageUrlLarge;

    @Column(name = "image_alt", length = 255)
    private String imageAlt;

    @Column(name = "spoiler_through_season_number")
    private Integer spoilerThroughSeasonNumber;

    @Column(name = "spoiler_through_episode_number")
    private Integer spoilerThroughEpisodeNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ContentDropStatus status = ContentDropStatus.DRAFT;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private long viewCount = 0L;

    // Türetilmiş alan: blok metinlerinden servis katmanında hesaplanır, client göndermez.
    @Column(name = "reading_time_minutes")
    private Integer readingTimeMinutes;

    // Aynı modül içi aggregate ilişkisi: gerçek JPA @OneToMany (Series->Season deseni)
    @OneToMany(mappedBy = "contentDrop", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    @ToString.Exclude
    @Builder.Default
    private List<ContentDropBlock> blocks = new ArrayList<>();

    @OneToMany(mappedBy = "contentDrop", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<ContentDropTag> tags = new ArrayList<>();

    // Yönlü: bu content drop sayfasının ALTINDA gösterilecek editöryel seçim.
    @OneToMany(mappedBy = "sourceContentDrop", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    @ToString.Exclude
    @Builder.Default
    private List<ContentDropRelation> outgoingRelations = new ArrayList<>();

    public void addBlock(ContentDropBlock block) {
        blocks.add(block);
        block.setContentDrop(this);
    }

    public void clearBlocks() {
        blocks.forEach(block -> block.setContentDrop(null));
        blocks.clear();
    }

    public void addTag(ContentDropTag tag) {
        tags.add(tag);
        tag.setContentDrop(this);
    }

    public void clearTags() {
        tags.forEach(tag -> tag.setContentDrop(null));
        tags.clear();
    }

    public void addOutgoingRelation(ContentDropRelation relation) {
        outgoingRelations.add(relation);
        relation.setSourceContentDrop(this);
    }

    public void clearOutgoingRelations() {
        outgoingRelations.forEach(relation -> relation.setSourceContentDrop(null));
        outgoingRelations.clear();
    }
}
