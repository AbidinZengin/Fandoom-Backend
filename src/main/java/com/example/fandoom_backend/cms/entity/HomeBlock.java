package com.example.fandoom_backend.cms.entity;

import com.example.fandoom_backend.content.entity.ContentBlock;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

// id/orderIndex/col/row ContentBlock'tan miras (bkz. content/entity/ContentBlock).
@Entity
@Table(name = "home_block")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString
public class HomeBlock extends ContentBlock {

    @Enumerated(EnumType.STRING)
    @Column(name = "page", nullable = false, length = 40)
    private PageName page;

    // page = SERIES_DETAIL/MOVIE_DETAIL/... gibi bir varlığa özel sayfaysa o varlığın id'si; sabit sayfalarda (HOME, GLOBAL vb.) null.
    @Column(name = "entity_id")
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "section", nullable = false, length = 40)
    private SectionName section;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ContentType contentType;

    @Column(name = "content_value", nullable = false, length = 2000)
    private String contentValue;

    @Column(name = "link_url", length = 2000)
    private String linkUrl;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
