package com.example.fandoom_backend.genre.mapper;

import com.example.fandoom_backend.genre.dto.GenreResponse;
import com.example.fandoom_backend.genre.entity.Genre;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GenreMapper {
    GenreResponse toResponse(Genre genre);
    List<GenreResponse> toResponseList(List<Genre> genres);
}
