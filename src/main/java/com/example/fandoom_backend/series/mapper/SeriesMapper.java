package com.example.fandoom_backend.series.mapper;

import com.example.fandoom_backend.series.dto.SeriesDetailResponse;
import com.example.fandoom_backend.series.dto.SeriesSummaryResponse;
import com.example.fandoom_backend.series.entity.Series;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = SeasonMapper.class)
public interface SeriesMapper {
    SeriesSummaryResponse toSummaryResponse(Series series);
    SeriesDetailResponse toDetailResponse(Series series);
    List<SeriesSummaryResponse> toSummaryResponseList(List<Series> seriesList);
}
