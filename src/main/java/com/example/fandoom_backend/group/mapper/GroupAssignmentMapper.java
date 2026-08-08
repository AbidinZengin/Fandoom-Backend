package com.example.fandoom_backend.group.mapper;

import com.example.fandoom_backend.group.dto.GroupAssignmentResponse;
import com.example.fandoom_backend.group.entity.GroupAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GroupAssignmentMapper {

    @Mapping(source = "group.id", target = "groupId")
    @Mapping(source = "group.name", target = "groupName")
    @Mapping(source = "group.slug", target = "groupSlug")
    @Mapping(source = "group.type", target = "groupType")
    GroupAssignmentResponse toResponse(GroupAssignment groupAssignment);

    List<GroupAssignmentResponse> toResponseList(List<GroupAssignment> groupAssignments);
}
