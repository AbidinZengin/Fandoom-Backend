package com.example.fandoom_backend.contentdrop.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

// Content drop sayfasının ALTINDA gösterilen "ilgili içerikler" şeridi için
// tamamen editöryel (elle seçilmiş+sıralanmış) ilişki — ContentDropTag'in
// algoritmik merdiveninden bağımsız. Yönlüdür: A, B'yi ilişkili gösterirse
// B'nin A'yı göstermesi otomatik gerekmez.
@Entity
@Table(name = "content_drop_relation", uniqueConstraints = {
        @UniqueConstraint(name = "uk_content_drop_relation_pair",
                columnNames = {"source_content_drop_id", "related_content_drop_id"})
}, indexes = {
        @Index(name = "idx_content_drop_relation_source", columnList = "source_content_drop_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class ContentDropRelation extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_content_drop_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_content_drop_relation_source"))
    @ToString.Exclude
    private ContentDrop sourceContentDrop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_content_drop_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_content_drop_relation_related"))
    @ToString.Exclude
    private ContentDrop relatedContentDrop;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;
}
