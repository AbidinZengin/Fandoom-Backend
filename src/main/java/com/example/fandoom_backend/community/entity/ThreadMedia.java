package com.example.fandoom_backend.community.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Thread'e bağlı sıralı medya (görsel/video). position liste sırasıdır (0..n), servis atar.
// (thread_id, position) üzerinde UNIQUE bilinçli olarak YOK: değiştirme = sil + yeniden ekle aynı
// transaction'da olur ve Hibernate IDENTITY insert'i bekleyen delete'ten önce çalıştırır (uk ihlali).
@Entity
@Table(name = "thread_media", indexes = {
        @Index(name = "idx_thread_media_thread_position", columnList = "thread_id, position"),
        @Index(name = "idx_thread_media_url", columnList = "url")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class ThreadMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_thread_media_thread"))
    @ToString.Exclude
    private Thread thread;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ThreadMediaType type;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false)
    private int position;
}
