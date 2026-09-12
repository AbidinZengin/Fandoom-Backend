package com.example.fandoom_backend.series.mapper;

import com.example.fandoom_backend.common.util.LocalizedTextResolver;
import com.example.fandoom_backend.series.dto.EpisodeBlockResponse;
import com.example.fandoom_backend.series.dto.EpisodeResponse;
import com.example.fandoom_backend.series.entity.Episode;
import com.example.fandoom_backend.series.entity.EpisodeBlock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", imports = LocalizedTextResolver.class)
public interface EpisodeMapper {

    @Mapping(target = "title", expression = "java(LocalizedTextResolver.resolve(episode.getTitleTr(), episode.getTitle()))")
    @Mapping(target = "synopsis", expression = "java(LocalizedTextResolver.resolve(episode.getSynopsisTr(), episode.getSynopsis()))")
    @Mapping(target = "storyTitle", expression = "java(LocalizedTextResolver.resolve(episode.getStoryTitleTr(), episode.getStoryTitle()))")
    EpisodeResponse toResponse(Episode episode);

    List<EpisodeResponse> toResponseList(List<Episode> episodes);

    @Mapping(target = "sceneKicker", expression = "java(LocalizedTextResolver.resolve(block.getSceneKickerTr(), block.getSceneKicker()))")
    @Mapping(target = "content", expression = "java(LocalizedTextResolver.resolve(block.getContentTr(), block.getContent()))")
    @Mapping(target = "mediaAlt", expression = "java(LocalizedTextResolver.resolve(block.getMediaAltTr(), block.getMediaAlt()))")
    EpisodeBlockResponse toBlockResponse(EpisodeBlock block);
}
