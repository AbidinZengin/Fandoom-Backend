package com.example.fandoom_backend.video.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "video",
        uniqueConstraints = @UniqueConstraint(name = "uk_video_item_youtube",
                columnNames = {"item_type", "item_id", "youtube_id"}),
        indexes = @Index(name = "idx_video_item_order", columnList = "item_type, item_id, sort_order, id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class Video extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Cross-module polimorfik referans (Trivia deseni): itemType hangi tabloya baktığını söyler, gerçek FK yok.
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, columnDefinition = "VARCHAR(20)")
    private VideoItemType itemType;

    // Oluşturulduktan sonra değişmez; değişecekse sil ve yeniden ekle.
    @Column(name = "youtube_id", nullable = false, length = 11, updatable = false)
    private String youtubeId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "channel_name", nullable = false, length = 120)
    private String channelName;

    @Column(name = "channel_url", nullable = false, length = 500)
    private String channelUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
