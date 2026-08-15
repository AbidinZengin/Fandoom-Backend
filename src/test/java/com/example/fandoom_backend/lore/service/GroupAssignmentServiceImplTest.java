package com.example.fandoom_backend.lore.service;

import com.example.fandoom_backend.common.exception.DuplicateResourceException;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.lore.dto.GroupAssignmentRequest;
import com.example.fandoom_backend.lore.dto.GroupAssignmentResponse;
import com.example.fandoom_backend.lore.entity.Group;
import com.example.fandoom_backend.lore.entity.GroupAssignment;
import com.example.fandoom_backend.lore.entity.TaggableType;
import com.example.fandoom_backend.lore.mapper.GroupAssignmentMapper;
import com.example.fandoom_backend.lore.repository.GroupAssignmentRepository;
import com.example.fandoom_backend.lore.repository.GroupRepository;
import com.example.fandoom_backend.lore.repository.LocationRepository;
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
class GroupAssignmentServiceImplTest {

    @Mock
    private GroupAssignmentRepository groupAssignmentRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private GroupAssignmentMapper groupAssignmentMapper;
    @Mock
    private CharacterService characterService;

    private GroupAssignmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GroupAssignmentServiceImpl(groupAssignmentRepository, groupRepository,
                locationRepository, groupAssignmentMapper, characterService);

        lenient().when(groupRepository.findById(1L))
                .thenReturn(Optional.of(Group.builder().id(1L).build()));
        lenient().when(groupAssignmentRepository.existsByGroupIdAndTaggableTypeAndTaggableId(any(), any(), any()))
                .thenReturn(false);
        lenient().when(groupAssignmentRepository.save(any(GroupAssignment.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(groupAssignmentMapper.toResponse(any()))
                .thenReturn(new GroupAssignmentResponse(1L, 1L, "House Stark", "house-stark",
                        TaggableType.CHARACTER, 42L));
    }

    @Test
    void assign_characterExists_savesAssignment() {
        when(characterService.existsById(42L)).thenReturn(true);
        GroupAssignmentRequest request = new GroupAssignmentRequest(TaggableType.CHARACTER, 42L);

        service.assign(1L, request);

        verify(characterService).existsById(42L);
        verify(groupAssignmentRepository).save(any(GroupAssignment.class));
    }

    @Test
    void assign_locationExists_savesAssignment() {
        when(locationRepository.existsById(7L)).thenReturn(true);
        GroupAssignmentRequest request = new GroupAssignmentRequest(TaggableType.LOCATION, 7L);

        service.assign(1L, request);

        verify(locationRepository).existsById(7L);
        verify(characterService, never()).existsById(any());
        verify(groupAssignmentRepository).save(any(GroupAssignment.class));
    }

    @Test
    void assign_characterDoesNotExist_throwsInvalidReferenceException() {
        when(characterService.existsById(99L)).thenReturn(false);
        GroupAssignmentRequest request = new GroupAssignmentRequest(TaggableType.CHARACTER, 99L);

        assertThatThrownBy(() -> service.assign(1L, request))
                .isInstanceOf(InvalidReferenceException.class);

        verify(groupAssignmentRepository, never()).save(any());
    }

    @Test
    void assign_duplicateAssignment_throwsDuplicateResourceException() {
        when(characterService.existsById(42L)).thenReturn(true);
        when(groupAssignmentRepository.existsByGroupIdAndTaggableTypeAndTaggableId(1L, TaggableType.CHARACTER, 42L))
                .thenReturn(true);
        GroupAssignmentRequest request = new GroupAssignmentRequest(TaggableType.CHARACTER, 42L);

        assertThatThrownBy(() -> service.assign(1L, request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(groupAssignmentRepository, never()).save(any());
    }

    @Test
    void assign_groupNotFound_throwsResourceNotFoundException() {
        when(groupRepository.findById(404L)).thenReturn(Optional.empty());
        GroupAssignmentRequest request = new GroupAssignmentRequest(TaggableType.CHARACTER, 42L);

        assertThatThrownBy(() -> service.assign(404L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(groupAssignmentRepository, never()).save(any());
    }

    @Test
    void delete_notFound_throwsResourceNotFoundException() {
        when(groupAssignmentRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(404L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(groupAssignmentRepository, never()).deleteById(any());
    }

    @Test
    void listForTarget_delegatesToRepository() {
        service.listForTarget(TaggableType.CHARACTER, 42L);

        verify(groupAssignmentRepository).findByTaggableTypeAndTaggableId(TaggableType.CHARACTER, 42L);
    }
}
