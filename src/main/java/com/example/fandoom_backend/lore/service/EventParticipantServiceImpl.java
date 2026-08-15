package com.example.fandoom_backend.lore.service;

import com.example.fandoom_backend.common.exception.DuplicateResourceException;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.lore.dto.EventParticipantRequest;
import com.example.fandoom_backend.lore.dto.EventParticipantResponse;
import com.example.fandoom_backend.lore.entity.Event;
import com.example.fandoom_backend.lore.entity.EventParticipant;
import com.example.fandoom_backend.lore.entity.ParticipantType;
import com.example.fandoom_backend.lore.mapper.EventParticipantMapper;
import com.example.fandoom_backend.lore.repository.EventParticipantRepository;
import com.example.fandoom_backend.lore.repository.EventRepository;
import com.example.fandoom_backend.lore.repository.GroupRepository;
import com.example.fandoom_backend.person.service.CharacterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventParticipantServiceImpl implements EventParticipantService {

    private final EventParticipantRepository eventParticipantRepository;
    private final EventRepository eventRepository;
    private final GroupRepository groupRepository;
    private final EventParticipantMapper eventParticipantMapper;
    private final CharacterService characterService;

    @Override
    public List<EventParticipantResponse> listForEvent(Long eventId) {
        return eventParticipantMapper.toResponseList(eventParticipantRepository.findByEventId(eventId));
    }

    @Override
    @Transactional
    public EventParticipantResponse assign(Long eventId, EventParticipantRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event bulunamadı: id=" + eventId));
        assertParticipantExists(request.participantType(), request.participantId());
        if (eventParticipantRepository.existsByEventIdAndParticipantTypeAndParticipantId(
                eventId, request.participantType(), request.participantId())) {
            throw new DuplicateResourceException("Bu katılımcı zaten bu olaya atanmış");
        }
        EventParticipant participant = EventParticipant.builder()
                .event(event)
                .participantType(request.participantType())
                .participantId(request.participantId())
                .build();
        return eventParticipantMapper.toResponse(eventParticipantRepository.save(participant));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!eventParticipantRepository.existsById(id)) {
            throw new ResourceNotFoundException("Event participant bulunamadı: id=" + id);
        }
        eventParticipantRepository.deleteById(id);
    }

    private void assertParticipantExists(ParticipantType participantType, Long participantId) {
        boolean exists = switch (participantType) {
            case CHARACTER -> characterService.existsById(participantId);
            case GROUP -> groupRepository.existsById(participantId);
        };
        if (!exists) {
            throw new InvalidReferenceException(
                    "Geçersiz " + participantType + " id: " + participantId);
        }
    }
}
