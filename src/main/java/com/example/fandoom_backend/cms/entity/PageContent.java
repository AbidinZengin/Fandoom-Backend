package com.example.fandoom_backend.cms.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "page_content")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class PageContent extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

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

    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private int orderIndex = 0;
}
