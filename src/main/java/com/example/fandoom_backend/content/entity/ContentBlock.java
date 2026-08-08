package com.example.fandoom_backend.content.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// blog/BlogBlock ve cms/HomeBlock'un ortak atası. Yalnızca gerçekten her
// alt tipte kullanılan alanları taşır: orderIndex + col/row (CSS grid
// pozisyonu, ör. "1 / 6" veya "2" — backend için opak, sadece saklanır/döner).
// Tipe özel alanlar (blockType/text/imageUrl, page/section/contentType...)
// alt sınıflarda kalır; buraya taşınmaz.
// Alt sınıflar equals/hashCode'da callSuper=true kullanmalı ki id burada
// kalsın (aksi halde onlyExplicitlyIncluded=true + callSuper=false ile
// alt sınıfın equals'ı hiçbir alanı karşılaştırmaz).
@Entity
@Table(name = "content_block")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class ContentBlock extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "col", length = 20)
    private String col;

    @Column(name = "row", length = 20)
    private String row;
}
