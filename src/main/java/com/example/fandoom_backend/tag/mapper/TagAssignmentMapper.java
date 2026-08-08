package com.example.fandoom_backend.tag.mapper;

import com.example.fandoom_backend.tag.dto.TagAssignmentResponse;
import com.example.fandoom_backend.tag.entity.TagAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TagAssignmentMapper {

    @Mapping(source = "tag.id", target = "tagId")
    @Mapping(source = "tag.name", target = "tagName")
    @Mapping(source = "tag.slug", target = "tagSlug")
    @Mapping(source = "tag.type", target = "tagType")
    TagAssignmentResponse toResponse(TagAssignment tagAssignment);

    List<TagAssignmentResponse> toResponseList(List<TagAssignment> tagAssignments);
}
