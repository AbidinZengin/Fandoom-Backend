package com.example.fandoom_backend.lore.mapper;

import com.example.fandoom_backend.lore.dto.GroupAssignmentResponse;
import com.example.fandoom_backend.lore.entity.GroupAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GroupAssignmentMapper {

    @Mapping(source = "group.id", target = "groupId")
    @Mapping(source = "group.name", target = "groupName")
    @Mapping(source = "group.slug", target = "groupSlug")
    GroupAssignmentResponse toResponse(GroupAssignment groupAssignment);

    List<GroupAssignmentResponse> toResponseList(List<GroupAssignment> groupAssignments);
}
