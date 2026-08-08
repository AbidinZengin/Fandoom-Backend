package com.example.fandoom_backend.content.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// blog/BlogBlock ve cms/HomeBlock'un ortak atası. Yalnızca gerçekten her
// alt tipte kullanılan alanları taşır: orderIndex + col/row (CSS grid
// pozisyonu, ör. "1 / 6" veya "2" — backend için opak, sadece saklanır/döner).
// Tipe özel alanlar (blockType/text/imageUrl, page/section/contentType...)
// alt sınıflarda kalır; buraya taşınmaz.
// Bilinçli olarak builder YOK (abstract, doğrudan inşa edilmez) — bu, ortak
// atadaki Auditable'a @SuperBuilder eklemekten kaçınır (o, Auditable'ı
// extend eden TÜM diğer entity'lerin (Franchise, Movie, ...) mevcut plain
// @Builder'ıyla static builder() dönüş tipi çakışması yaratıp derlemeyi
// kırıyordu). Alt sınıflar kendi alanları için plain @Builder kullanır,
// buradan miras alınan orderIndex/col/row servis katmanında setter ile
// atanır (bkz. BlogServiceImpl.applyBlocks, HomeBlockServiceImpl).
// Alt sınıflar equals/hashCode'da callSuper=true kullanmalı ki id burada
// kalsın (aksi halde onlyExplicitlyIncluded=true + callSuper=false ile
// alt sınıfın equals'ı hiçbir alanı karşılaştırmaz).
@Entity
@Table(name = "content_block")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
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

    // sütun adı bilerek "row" değil "grid_row" — ROW, MySQL 8.0.19+'da
    // rezerve kelime; Java/DTO alan adı row olarak kalıyor, API sözleşmesi
    // değişmiyor.
    @Column(name = "grid_row", length = 20)
    private String row;
}
