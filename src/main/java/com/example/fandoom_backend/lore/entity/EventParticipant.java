package com.example.fandoom_backend.lore.entity;

import com.example.fandoom_backend.common.entity.Auditable;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Bir olayın katılımcısı — hem bireysel karakterler hem haneler/gruplar bir
// olaya atanabilsin diye çok biçimli (polymorphic) tutuldu, GroupAssignment'ın
// taggableType/taggableId deseniyle aynı mantık.
@Entity
@Table(name = "event_participant", uniqueConstraints = {
        @UniqueConstraint(name = "uk_event_participant",
                columnNames = {"event_id", "participant_type", "participant_id"})
}, indexes = {
        @Index(name = "idx_event_participant_target", columnList = "participant_type, participant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class EventParticipant extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_event_participant_event"))
    @ToString.Exclude
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false, length = 20)
    private ParticipantType participantType;

    // Cross-module (CHARACTER) veya aynı-modül (GROUP) referans — tutarlılık
    // için ikisi de ID-only, JPA ilişkisi YOK.
    @Column(name = "participant_id", nullable = false)
    private Long participantId;
}
