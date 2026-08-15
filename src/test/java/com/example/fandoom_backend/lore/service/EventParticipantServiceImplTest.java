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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventParticipantServiceImplTest {

    @Mock
    private EventParticipantRepository eventParticipantRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private EventParticipantMapper eventParticipantMapper;
    @Mock
    private CharacterService characterService;

    private EventParticipantServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EventParticipantServiceImpl(eventParticipantRepository, eventRepository,
                groupRepository, eventParticipantMapper, characterService);

        lenient().when(eventRepository.findById(1L)).thenReturn(Optional.of(Event.builder().id(1L).build()));
        lenient().when(eventParticipantRepository.existsByEventIdAndParticipantTypeAndParticipantId(any(), any(), any()))
                .thenReturn(false);
        lenient().when(eventParticipantRepository.save(any(EventParticipant.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(eventParticipantMapper.toResponse(any()))
                .thenReturn(new EventParticipantResponse(1L, 1L, ParticipantType.CHARACTER, 42L));
    }

    @Test
    void assign_characterParticipant_validatesViaCharacterService() {
        when(characterService.existsById(42L)).thenReturn(true);

        service.assign(1L, new EventParticipantRequest(ParticipantType.CHARACTER, 42L));

        verify(characterService).existsById(42L);
        verify(groupRepository, never()).existsById(any());
        verify(eventParticipantRepository).save(any(EventParticipant.class));
    }

    @Test
    void assign_groupParticipant_validatesViaGroupRepository() {
        when(groupRepository.existsById(7L)).thenReturn(true);

        service.assign(1L, new EventParticipantRequest(ParticipantType.GROUP, 7L));

        verify(groupRepository).existsById(7L);
        verify(characterService, never()).existsById(any());
        verify(eventParticipantRepository).save(any(EventParticipant.class));
    }

    @Test
    void assign_invalidParticipant_throwsInvalidReferenceException() {
        when(characterService.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.assign(1L, new EventParticipantRequest(ParticipantType.CHARACTER, 99L)))
                .isInstanceOf(InvalidReferenceException.class);

        verify(eventParticipantRepository, never()).save(any());
    }

    @Test
    void assign_duplicateParticipant_throwsDuplicateResourceException() {
        when(characterService.existsById(42L)).thenReturn(true);
        when(eventParticipantRepository.existsByEventIdAndParticipantTypeAndParticipantId(
                1L, ParticipantType.CHARACTER, 42L)).thenReturn(true);

        assertThatThrownBy(() -> service.assign(1L, new EventParticipantRequest(ParticipantType.CHARACTER, 42L)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(eventParticipantRepository, never()).save(any());
    }

    @Test
    void assign_eventNotFound_throwsResourceNotFoundException() {
        when(eventRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign(404L, new EventParticipantRequest(ParticipantType.CHARACTER, 42L)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventParticipantRepository, never()).save(any());
    }

    @Test
    void delete_notFound_throwsResourceNotFoundException() {
        when(eventParticipantRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
