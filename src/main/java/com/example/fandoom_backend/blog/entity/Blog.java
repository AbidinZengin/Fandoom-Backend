package com.example.fandoom_backend.blog.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "blog", uniqueConstraints = {
        @UniqueConstraint(name = "uk_blog_slug", columnNames = "slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class Blog extends Auditable {

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
    private BlogStatus status = BlogStatus.DRAFT;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private long viewCount = 0L;

    // Türetilmiş alan: blok metinlerinden servis katmanında hesaplanır, client göndermez.
    @Column(name = "reading_time_minutes")
    private Integer readingTimeMinutes;

    // Aynı modül içi aggregate ilişkisi: gerçek JPA @OneToMany (Series->Season deseni)
    @OneToMany(mappedBy = "blog", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    @ToString.Exclude
    @Builder.Default
    private List<BlogBlock> blocks = new ArrayList<>();

    @OneToMany(mappedBy = "blog", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<BlogTag> tags = new ArrayList<>();

    // Yönlü: bu blog sayfasının ALTINDA gösterilecek editöryel seçim.
    @OneToMany(mappedBy = "sourceBlog", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    @ToString.Exclude
    @Builder.Default
    private List<BlogRelation> outgoingRelations = new ArrayList<>();

    public void addBlock(BlogBlock block) {
        blocks.add(block);
        block.setBlog(this);
    }

    public void clearBlocks() {
        blocks.forEach(block -> block.setBlog(null));
        blocks.clear();
    }

    public void addTag(BlogTag tag) {
        tags.add(tag);
        tag.setBlog(this);
    }

    public void clearTags() {
        tags.forEach(tag -> tag.setBlog(null));
        tags.clear();
    }

    public void addOutgoingRelation(BlogRelation relation) {
        outgoingRelations.add(relation);
        relation.setSourceBlog(this);
    }

    public void clearOutgoingRelations() {
        outgoingRelations.forEach(relation -> relation.setSourceBlog(null));
        outgoingRelations.clear();
    }
}
