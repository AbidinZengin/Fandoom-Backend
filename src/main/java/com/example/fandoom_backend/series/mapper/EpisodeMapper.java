package com.example.fandoom_backend.series.mapper;

import com.example.fandoom_backend.series.dto.EpisodeResponse;
import com.example.fandoom_backend.series.entity.Episode;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EpisodeMapper {
    EpisodeResponse toResponse(Episode episode);
    List<EpisodeResponse> toResponseList(List<Episode> episodes);
}
