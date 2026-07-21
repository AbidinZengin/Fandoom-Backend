package com.example.fandoom_backend.series.service;

import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.series.dto.SeasonDetailResponse;
import com.example.fandoom_backend.series.dto.SeasonRequest;
import com.example.fandoom_backend.series.dto.SeasonSummaryResponse;
import com.example.fandoom_backend.series.entity.Season;
import com.example.fandoom_backend.series.entity.Series;
import com.example.fandoom_backend.series.mapper.SeasonMapper;
import com.example.fandoom_backend.series.repository.SeasonRepository;
import com.example.fandoom_backend.series.repository.SeriesRepository;
import com.example.fandoom_backend.media.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeasonServiceImpl implements SeasonService {

    private final SeasonRepository seasonRepository;
    private final SeriesRepository seriesRepository;
    private final SeasonMapper seasonMapper;
    private final ImageStorageService imageStorageService;

    @Override
    public List<SeasonSummaryResponse> listBySeries(Long seriesId) {
        return seasonMapper.toSummaryResponseList(
                seasonRepository.findBySeriesIdOrderBySeasonNumberAsc(seriesId));
    }

    @Override
    public SeasonDetailResponse getById(Long id) {
        return seasonMapper.toDetailResponse(findEntityById(id));
    }

    @Override
    @Transactional
    public SeasonDetailResponse create(Long seriesId, SeasonRequest request) {
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new ResourceNotFoundException("Series bulunamadı: id=" + seriesId));
        Season season = Season.builder()
                .seasonNumber(request.seasonNumber())
                .title(request.title())
                .posterUrl(request.posterUrl())
                .build();
        series.addSeason(season);
        seriesRepository.save(series);
        return seasonMapper.toDetailResponse(season);
    }

    @Override
    @Transactional
    public List<SeasonDetailResponse> createBatch(Long seriesId, List<SeasonRequest> requests) {
        return requests.stream()
                .map(request -> create(seriesId, request))
                .toList();
    }

    @Override
    @Transactional
    public SeasonDetailResponse update(Long id, SeasonRequest request) {
        Season season = findEntityById(id);
        imageStorageService.deleteIfChanged(season.getPosterUrl(), request.posterUrl());
        season.setSeasonNumber(request.seasonNumber());
        season.setTitle(request.title());
        season.setPosterUrl(request.posterUrl());
        return seasonMapper.toDetailResponse(season);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Season season = findEntityById(id);
        imageStorageService.delete(season.getPosterUrl());
        season.getSeries().removeSeason(season);
    }

    private Season findEntityById(Long id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Season bulunamadı: id=" + id));
    }
}
