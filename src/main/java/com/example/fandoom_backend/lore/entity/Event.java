package com.example.fandoom_backend.lore.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

// Bir yapımın zaman çizelgesindeki bir olay (ör. "Kral Katliamı"). Gerçek
// takvim tarihi YOK — hayali evren takvimleri (ör. "283 AC") gerçek bir
// java.time tipine düzgün eşlenemez; sıralama bilinçli olarak orderIndex ile
// çözüldü (ContentBlock/BlogBlock'taki orderIndex deseniyle aynı, YAGNI).
@Entity
@Table(name = "event", indexes = {
        @Index(name = "idx_event_subject", columnList = "subject_type, subject_id"),
        @Index(name = "idx_event_location", columnList = "location_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class Event extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // Filtrelenebilir olasılığı yüksek gerçek alan (EpisodeBlock.pinned emsaliyle
    // tutarlı) — customFields'a gömülmedi, "sadece pinlenmiş olayları getir"
    // gibi bir ihtiyaç çıkarsa hazır olsun diye.
    @Column(name = "pinned", nullable = false)
    private boolean pinned;

    // Sunuma özgü, yapıma özel alanlar (ör. görünen tarih metni "ca. 12,000 BC",
    // alıntı, ton, birincil locationId dışındaki ek yer adları) — backend
    // içeriği doğrulamaz/yorumlamaz, opak JSON blob (Group/Location.customFields
    // ile aynı felsefe). Kronolojik SIRALAMA için buraya bir "sortValue" KONMAZ
    // — o iş zaten orderIndex'in görevi, iki ayrı sıralama kaynağı çelişkiye yol açar.
    @Column(name = "custom_fields", columnDefinition = "json")
    private String customFields;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 20)
    private SubjectType subjectType;

    // Cross-module referans: Movie ya da Series id'si, JPA ilişkisi YOK
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    // Aynı modül içi ilişki -> gerçek FK, nullable (her olayın bir yeri olmak zorunda değil)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", foreignKey = @ForeignKey(name = "fk_event_location"))
    @ToString.Exclude
    private Location location;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    @Builder.Default
    @ToString.Exclude
    private List<EventParticipant> participants = new ArrayList<>();
}
