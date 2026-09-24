package com.example.fandoom_backend.trivia.mapper;

import com.example.fandoom_backend.common.util.LocalizedTextResolver;
import com.example.fandoom_backend.trivia.dto.TriviaResponse;
import com.example.fandoom_backend.trivia.entity.Trivia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", imports = LocalizedTextResolver.class)
public interface TriviaMapper {

    @Mapping(target = "isSpoiler", source = "spoiler")
    @Mapping(target = "title", expression = "java(LocalizedTextResolver.resolve(trivia.getTitleTr(), trivia.getTitle()))")
    @Mapping(target = "content", expression = "java(LocalizedTextResolver.resolve(trivia.getContentTr(), trivia.getContent()))")
    TriviaResponse toResponse(Trivia trivia);

    List<TriviaResponse> toResponseList(List<Trivia> trivia);
}
