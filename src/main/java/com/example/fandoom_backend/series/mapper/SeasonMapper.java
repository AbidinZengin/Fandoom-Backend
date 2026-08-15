package com.example.fandoom_backend.series.mapper;

import com.example.fandoom_backend.common.util.LocalizedTextResolver;
import com.example.fandoom_backend.series.dto.SeasonDetailResponse;
import com.example.fandoom_backend.series.dto.SeasonSummaryResponse;
import com.example.fandoom_backend.series.entity.Season;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = EpisodeMapper.class, imports = LocalizedTextResolver.class)
public interface SeasonMapper {

    @Mapping(target = "title", expression = "java(LocalizedTextResolver.resolve(season.getTitleTr(), season.getTitle()))")
    SeasonSummaryResponse toSummaryResponse(Season season);

    @Mapping(target = "title", expression = "java(LocalizedTextResolver.resolve(season.getTitleTr(), season.getTitle()))")
    SeasonDetailResponse toDetailResponse(Season season);

    List<SeasonSummaryResponse> toSummaryResponseList(List<Season> seasons);
}
