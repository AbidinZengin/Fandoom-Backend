package com.example.fandoom_backend.trivia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Trivia etiket tanımı (kapalı küme). Trivia'nın etiketleri yalnız bu tablodaki `code` değerlerinden
 * biri olabilir; bilinmeyen kod POST/PUT'ta 400 döner. Trivia tarafında etiketler `trivia_tags`
 * tablosunda düz `code` string'i olarak saklanır (JPA'da @ElementCollection); `trivia_tags.tag` →
 * `trivia_tag.code` FK'sı migration scriptiyle (trivia_tag_table_migration.sql) kurulur.
 * Yeni etiket eklemek için bu tabloya satır eklenir.
 */
@Entity
@Table(name = "trivia_tag", uniqueConstraints = @UniqueConstraint(name = "uk_trivia_tag_code", columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@DynamicUpdate
public class TriviaTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Trivia yanıtındaki `tag`/`tags[]` değeri. Hazır etiketler BÜYÜK_HARF_ALT_ÇİZGİ (FE ikon eşlemesi bunlara
    // bakar), diğerleri okunur biçimde (ör. "Costume"). Tekil (büyük/küçük harf duyarsız).
    @Column(nullable = false, length = 30)
    private String code;

    // Movie.title/titleTr deseni: EN ana alan, TR karşılığı; yanıtta `label` istek diline göre çözülür.
    @Column(name = "label_en", nullable = false, length = 60)
    private String labelEn;

    @Column(name = "label_tr", nullable = false, length = 60)
    private String labelTr;
}
