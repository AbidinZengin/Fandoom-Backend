package com.example.fandoom_backend.lore.mapper;

import com.example.fandoom_backend.lore.dto.GroupResponse;
import com.example.fandoom_backend.lore.entity.Group;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GroupMapper {

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "category.slug", target = "categorySlug")
    @Mapping(source = "category.subjectType", target = "subjectType")
    @Mapping(source = "category.subjectId", target = "subjectId")
    GroupResponse toResponse(Group group);

    List<GroupResponse> toResponseList(List<Group> groups);
}
