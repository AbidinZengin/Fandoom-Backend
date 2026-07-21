package com.example.fandoom_backend.series.mapper;

import com.example.fandoom_backend.series.dto.SeasonDetailResponse;
import com.example.fandoom_backend.series.dto.SeasonSummaryResponse;
import com.example.fandoom_backend.series.entity.Season;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = EpisodeMapper.class)
public interface SeasonMapper {
    SeasonSummaryResponse toSummaryResponse(Season season);
    SeasonDetailResponse toDetailResponse(Season season);
    List<SeasonSummaryResponse> toSummaryResponseList(List<Season> seasons);
}
