package com.example.fandoom_backend.person.mapper;

import com.example.fandoom_backend.person.dto.CastResponse;
import com.example.fandoom_backend.person.entity.Cast;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PersonMapper.class, CharacterMapper.class})
public interface CastMapper {
    CastResponse toResponse(Cast cast);
    List<CastResponse> toResponseList(List<Cast> casts);
}
