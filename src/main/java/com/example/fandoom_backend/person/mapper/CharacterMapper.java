package com.example.fandoom_backend.person.mapper;

import com.example.fandoom_backend.person.dto.CharacterResponse;
import com.example.fandoom_backend.person.entity.Character;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CharacterMapper {
    CharacterResponse toResponse(Character character);
    List<CharacterResponse> toResponseList(List<Character> characters);
}
