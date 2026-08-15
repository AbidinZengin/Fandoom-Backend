package com.example.fandoom_backend.lore.repository;

import com.example.fandoom_backend.lore.entity.EventParticipant;
import com.example.fandoom_backend.lore.entity.ParticipantType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {
    List<EventParticipant> findByEventId(Long eventId);
    boolean existsByEventIdAndParticipantTypeAndParticipantId(
            Long eventId, ParticipantType participantType, Long participantId);
}
