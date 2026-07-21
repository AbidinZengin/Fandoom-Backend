package com.example.fandoom_backend.series.service;

import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.series.dto.EpisodeRequest;
import com.example.fandoom_backend.series.dto.EpisodeResponse;
import com.example.fandoom_backend.series.entity.Episode;
import com.example.fandoom_backend.series.entity.Season;
import com.example.fandoom_backend.series.mapper.EpisodeMapper;
import com.example.fandoom_backend.series.repository.EpisodeRepository;
import com.example.fandoom_backend.series.repository.SeasonRepository;
import com.example.fandoom_backend.media.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EpisodeServiceImpl implements EpisodeService {

    private final EpisodeRepository episodeRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeMapper episodeMapper;
    private final ImageStorageService imageStorageService;

    @Override
    public List<EpisodeResponse> listBySeason(Long seasonId) {
        return episodeMapper.toResponseList(
                episodeRepository.findBySeasonIdOrderByEpisodeNumberAsc(seasonId));
    }

    @Override
    public EpisodeResponse getById(Long id) {
        return episodeMapper.toResponse(findEntityById(id));
    }

    @Override
    @Transactional
    public EpisodeResponse create(Long seasonId, EpisodeRequest request) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Season bulunamadı: id=" + seasonId));
        Episode episode = Episode.builder()
                .episodeNumber(request.episodeNumber())
                .title(request.title())
                .synopsis(request.synopsis())
                .airDate(request.airDate())
                .durationMinutes(request.durationMinutes())
                .stillImageUrl(request.stillImageUrl())
                .build();
        season.addEpisode(episode);
        seasonRepository.save(season);
        return episodeMapper.toResponse(episode);
    }

    @Override
    @Transactional
    public List<EpisodeResponse> createBatch(Long seasonId, List<EpisodeRequest> requests) {
        return requests.stream()
                .map(request -> create(seasonId, request))
                .toList();
    }

    @Override
    @Transactional
    public EpisodeResponse update(Long id, EpisodeRequest request) {
        Episode episode = findEntityById(id);
        imageStorageService.deleteIfChanged(episode.getStillImageUrl(), request.stillImageUrl());
        episode.setEpisodeNumber(request.episodeNumber());
        episode.setTitle(request.title());
        episode.setSynopsis(request.synopsis());
        episode.setAirDate(request.airDate());
        episode.setDurationMinutes(request.durationMinutes());
        episode.setStillImageUrl(request.stillImageUrl());
        return episodeMapper.toResponse(episode);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Episode episode = findEntityById(id);
        imageStorageService.delete(episode.getStillImageUrl());
        episode.getSeason().removeEpisode(episode);
    }

    private Episode findEntityById(Long id) {
        return episodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Episode bulunamadı: id=" + id));
    }
}
