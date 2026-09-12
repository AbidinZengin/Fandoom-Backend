package com.example.fandoom_backend.community.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Ayrı bir master Tag entity YOK — kasıtlı. tag/ modülü (Tag+TagAssignment,
// editor-curated, yazma EDITOR/MODERATOR/ADMIN rolüyle kısıtlı) burada
// kullanılmıyor: kullanıcı kendi thread'ine serbestçe tag yazabilmeli, bu da
// tag/ modülünün rol modeliyle uyuşmuyor. tag alanı SlugGenerator.slugify ile
// normalize edilir (ThreadServiceImpl.applyTags), regex/format doğrulaması
// burada tekrarlanmaz.
@Entity
@Table(name = "thread_tag", uniqueConstraints = {
        @UniqueConstraint(name = "uk_thread_tag", columnNames = {"thread_id", "tag"})
}, indexes = {
        @Index(name = "idx_thread_tag_tag", columnList = "tag")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class ThreadTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_thread_tag_thread"))
    @ToString.Exclude
    private Thread thread;

    @Column(nullable = false, length = 50)
    private String tag;
}
