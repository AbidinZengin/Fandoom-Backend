package com.example.fandoom_backend.group.mapper;

import com.example.fandoom_backend.group.dto.GroupResponse;
import com.example.fandoom_backend.group.entity.Group;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GroupMapper {
    GroupResponse toResponse(Group group);
    List<GroupResponse> toResponseList(List<Group> groups);
}
