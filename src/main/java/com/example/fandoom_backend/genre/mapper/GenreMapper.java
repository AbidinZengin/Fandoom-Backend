package com.example.fandoom_backend.genre.mapper;

import com.example.fandoom_backend.common.util.LocalizedTextResolver;
import com.example.fandoom_backend.genre.dto.GenreResponse;
import com.example.fandoom_backend.genre.entity.Genre;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", imports = LocalizedTextResolver.class)
public interface GenreMapper {

    @Mapping(target = "name", expression = "java(LocalizedTextResolver.resolve(genre.getNameTr(), genre.getName()))")
    GenreResponse toResponse(Genre genre);

    List<GenreResponse> toResponseList(List<Genre> genres);
}
