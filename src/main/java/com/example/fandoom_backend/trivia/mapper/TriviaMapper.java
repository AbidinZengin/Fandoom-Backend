package com.example.fandoom_backend.trivia.mapper;

import com.example.fandoom_backend.common.util.LocalizedTextResolver;
import com.example.fandoom_backend.trivia.dto.TriviaResponse;
import com.example.fandoom_backend.trivia.dto.TriviaTagResponse;
import com.example.fandoom_backend.trivia.entity.Trivia;
import com.example.fandoom_backend.trivia.entity.TriviaTag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", imports = LocalizedTextResolver.class)
public interface TriviaMapper {

    @Mapping(target = "isSpoiler", source = "spoiler")
    @Mapping(target = "title", expression = "java(LocalizedTextResolver.resolve(trivia.getTitleTr(), trivia.getTitle()))")
    @Mapping(target = "content", expression = "java(LocalizedTextResolver.resolve(trivia.getContentTr(), trivia.getContent()))")
    @Mapping(target = "tag", expression = "java(trivia.getTags().isEmpty() ? null : trivia.getTags().get(0))")
    TriviaResponse toResponse(Trivia trivia);

    List<TriviaResponse> toResponseList(List<Trivia> trivia);

    @Mapping(target = "label", expression = "java(LocalizedTextResolver.resolve(tag.getLabelTr(), tag.getLabelEn()))")
    TriviaTagResponse toTagResponse(TriviaTag tag);

    List<TriviaTagResponse> toTagResponseList(List<TriviaTag> tags);
}
