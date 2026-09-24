package com.example.fandoom_backend.trivia.mapper;

import com.example.fandoom_backend.trivia.dto.TriviaResponse;
import com.example.fandoom_backend.trivia.entity.Trivia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TriviaMapper {

    @Mapping(target = "isSpoiler", source = "spoiler")
    TriviaResponse toResponse(Trivia trivia);

    List<TriviaResponse> toResponseList(List<Trivia> trivia);
}
