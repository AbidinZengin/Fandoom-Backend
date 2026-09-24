package com.example.fandoom_backend.trivia.entity;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "trivia", indexes = {
        // Frontend (itemId, itemType) ikilisiyle çok sık okur; created_at sona eklendi ki
        // varsayılan (yeniden eskiye) sıralama da indeksten gelsin.
        @Index(name = "idx_trivia_item", columnList = "item_id, item_type, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class Trivia extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Cross-module polimorfik referans: itemType hangi tabloya baktığını söyler, gerçek FK yok.
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    // VARCHAR açıkça zorlanır: Hibernate'e bırakılınca MySQL native enum(...) üretiyor ve yeni
    // değer eklemek ddl-auto=update ile ALTER edilmiyor (bkz. Movie.status notu).
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, columnDefinition = "VARCHAR(20)")
    private TriviaItemType itemType;

    // Movie.title/titleTr deseni: `title`/`content` ana (EN) alan ve zorunlu, *Tr opsiyonel.
    @Column(length = 255)
    private String title;

    @Column(name = "title_tr", length = 255)
    private String titleTr;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_tr", columnDefinition = "TEXT")
    private String contentTr;

    // media/'den dönen düz URL (bağımsızlık ilkesi); silme/değişimde ImageStorageService temizler.
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    private TriviaTag tag;

    @Column(name = "is_spoiler", nullable = false)
    private boolean spoiler;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    // Cross-module: user/ modülüne düz ID (FK yok). Kullanıcı silinse de kayıt kalır.
    @Column(name = "created_by")
    private Long createdBy;
}
