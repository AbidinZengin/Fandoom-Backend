package com.example.fandoom_backend.group.service;

import com.example.fandoom_backend.common.exception.DuplicateResourceException;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.group.dto.GroupAssignmentRequest;
import com.example.fandoom_backend.group.dto.GroupAssignmentResponse;
import com.example.fandoom_backend.group.entity.Group;
import com.example.fandoom_backend.group.entity.GroupAssignment;
import com.example.fandoom_backend.group.entity.TaggableType;
import com.example.fandoom_backend.group.mapper.GroupAssignmentMapper;
import com.example.fandoom_backend.group.repository.GroupAssignmentRepository;
import com.example.fandoom_backend.group.repository.GroupRepository;
import com.example.fandoom_backend.person.service.CharacterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupAssignmentServiceImpl implements GroupAssignmentService {

    private final GroupAssignmentRepository groupAssignmentRepository;
    private final GroupRepository groupRepository;
    private final GroupAssignmentMapper groupAssignmentMapper;
    private final CharacterService characterService;

    @Override
    public List<GroupAssignmentResponse> listForTarget(TaggableType taggableType, Long taggableId) {
        return groupAssignmentMapper.toResponseList(
                groupAssignmentRepository.findByTaggableTypeAndTaggableId(taggableType, taggableId));
    }

    @Override
    @Transactional
    public GroupAssignmentResponse assign(Long groupId, GroupAssignmentRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group bulunamadı: id=" + groupId));
        assertTaggableExists(request.taggableType(), request.taggableId());
        if (groupAssignmentRepository.existsByGroupIdAndTaggableTypeAndTaggableId(
                groupId, request.taggableType(), request.taggableId())) {
            throw new DuplicateResourceException("Bu grup zaten bu hedefe atanmış");
        }
        GroupAssignment groupAssignment = GroupAssignment.builder()
                .group(group)
                .taggableType(request.taggableType())
                .taggableId(request.taggableId())
                .build();
        return groupAssignmentMapper.toResponse(groupAssignmentRepository.save(groupAssignment));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!groupAssignmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Group assignment bulunamadı: id=" + id);
        }
        groupAssignmentRepository.deleteById(id);
    }

    private void assertTaggableExists(TaggableType taggableType, Long taggableId) {
        boolean exists = switch (taggableType) {
            case CHARACTER -> characterService.existsById(taggableId);
        };
        if (!exists) {
            throw new InvalidReferenceException(
                    "Geçersiz " + taggableType + " id: " + taggableId);
        }
    }
}
