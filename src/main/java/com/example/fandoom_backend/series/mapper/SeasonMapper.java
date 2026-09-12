package com.example.fandoom_backend.series.mapper;

import com.example.fandoom_backend.common.util.LocalizedTextResolver;
import com.example.fandoom_backend.series.dto.SeasonBlockResponse;
import com.example.fandoom_backend.series.dto.SeasonDetailResponse;
import com.example.fandoom_backend.series.dto.SeasonSummaryResponse;
import com.example.fandoom_backend.series.entity.Season;
import com.example.fandoom_backend.series.entity.SeasonBlock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = EpisodeMapper.class, imports = LocalizedTextResolver.class)
public interface SeasonMapper {

    @Mapping(target = "title", expression = "java(LocalizedTextResolver.resolve(season.getTitleTr(), season.getTitle()))")
    @Mapping(target = "storyDek", expression = "java(LocalizedTextResolver.resolve(season.getStoryDekTr(), season.getStoryDek()))")
    SeasonSummaryResponse toSummaryResponse(Season season);

    @Mapping(target = "title", expression = "java(LocalizedTextResolver.resolve(season.getTitleTr(), season.getTitle()))")
    @Mapping(target = "storyKicker", expression = "java(LocalizedTextResolver.resolve(season.getStoryKickerTr(), season.getStoryKicker()))")
    @Mapping(target = "storyTitle", expression = "java(LocalizedTextResolver.resolve(season.getStoryTitleTr(), season.getStoryTitle()))")
    @Mapping(target = "storyDek", expression = "java(LocalizedTextResolver.resolve(season.getStoryDekTr(), season.getStoryDek()))")
    SeasonDetailResponse toDetailResponse(Season season);

    List<SeasonSummaryResponse> toSummaryResponseList(List<Season> seasons);

    @Mapping(target = "content", expression = "java(LocalizedTextResolver.resolve(block.getContentTr(), block.getContent()))")
    @Mapping(target = "mediaCaption", expression = "java(LocalizedTextResolver.resolve(block.getMediaCaptionTr(), block.getMediaCaption()))")
    SeasonBlockResponse toBlockResponse(SeasonBlock block);
}
