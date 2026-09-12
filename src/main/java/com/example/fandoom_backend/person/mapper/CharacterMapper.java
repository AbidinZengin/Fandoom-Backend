package com.example.fandoom_backend.person.mapper;

import com.example.fandoom_backend.common.util.LocalizedTextResolver;
import com.example.fandoom_backend.person.dto.CharacterResponse;
import com.example.fandoom_backend.person.entity.Character;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", imports = LocalizedTextResolver.class)
public interface CharacterMapper {

    @Mapping(target = "description", expression = "java(LocalizedTextResolver.resolve(character.getDescriptionTr(), character.getDescription()))")
    CharacterResponse toResponse(Character character);

    List<CharacterResponse> toResponseList(List<Character> characters);
}
