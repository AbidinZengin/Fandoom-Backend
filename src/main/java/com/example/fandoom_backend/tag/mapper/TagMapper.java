package com.example.fandoom_backend.tag.mapper;

import com.example.fandoom_backend.tag.dto.TagResponse;
import com.example.fandoom_backend.tag.entity.Tag;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TagMapper {
    TagResponse toResponse(Tag tag);
    List<TagResponse> toResponseList(List<Tag> tags);
}
