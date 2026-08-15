package com.example.fandoom_backend.series.mapper;

import com.example.fandoom_backend.common.util.LocalizedTextResolver;
import com.example.fandoom_backend.series.dto.SeriesDetailResponse;
import com.example.fandoom_backend.series.dto.SeriesSummaryResponse;
import com.example.fandoom_backend.series.entity.Series;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = SeasonMapper.class, imports = LocalizedTextResolver.class)
public interface SeriesMapper {

    @Mapping(target = "title", expression = "java(LocalizedTextResolver.resolve(series.getTitleTr(), series.getTitle()))")
    SeriesSummaryResponse toSummaryResponse(Series series);

    @Mapping(target = "title", expression = "java(LocalizedTextResolver.resolve(series.getTitleTr(), series.getTitle()))")
    @Mapping(target = "synopsis", expression = "java(LocalizedTextResolver.resolve(series.getSynopsisTr(), series.getSynopsis()))")
    SeriesDetailResponse toDetailResponse(Series series);

    List<SeriesSummaryResponse> toSummaryResponseList(List<Series> seriesList);
}
