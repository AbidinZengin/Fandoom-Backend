package com.example.fandoom_backend.blog.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

// Blog sayfasının ALTINDA gösterilen "ilgili içerikler" şeridi için
// tamamen editöryel (elle seçilmiş+sıralanmış) ilişki — BlogTag'in
// algoritmik merdiveninden bağımsız. Yönlüdür: A, B'yi ilişkili gösterirse
// B'nin A'yı göstermesi otomatik gerekmez.
@Entity
@Table(name = "blog_relation", uniqueConstraints = {
        @UniqueConstraint(name = "uk_blog_relation_pair",
                columnNames = {"source_blog_id", "related_blog_id"})
}, indexes = {
        @Index(name = "idx_blog_relation_source", columnList = "source_blog_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class BlogRelation extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_blog_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_blog_relation_source"))
    @ToString.Exclude
    private Blog sourceBlog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_blog_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_blog_relation_related"))
    @ToString.Exclude
    private Blog relatedBlog;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;
}
