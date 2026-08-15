package com.example.fandoom_backend.series.service;

import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.series.dto.EpisodeBlockRequest;
import com.example.fandoom_backend.series.dto.EpisodeRequest;
import com.example.fandoom_backend.series.dto.EpisodeResponse;
import com.example.fandoom_backend.series.entity.Episode;
import com.example.fandoom_backend.series.entity.EpisodeBlock;
import com.example.fandoom_backend.series.entity.Season;
import com.example.fandoom_backend.series.mapper.EpisodeMapper;
import com.example.fandoom_backend.series.repository.EpisodeRepository;
import com.example.fandoom_backend.series.repository.SeasonRepository;
import com.example.fandoom_backend.media.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
                .titleTr(request.titleTr())
                .title(request.title())
                .synopsisTr(request.synopsisTr())
                .synopsis(request.synopsis())
                .airDate(request.airDate())
                .durationMinutes(request.durationMinutes())
                .stillImageUrl(request.stillImageUrl())
                .externalRating(request.externalRating())
                .externalVoteCount(request.externalVoteCount())
                .externalRatingUpdatedAt(resolveExternalRatingUpdatedAt(request, null))
                .imdbId(request.imdbId())
                .tmdbId(request.tmdbId())
                .storyKickerTr(request.storyKickerTr())
                .storyKicker(request.storyKicker())
                .storyTitleTr(request.storyTitleTr())
                .storyTitle(request.storyTitle())
                .storyThesisTr(request.storyThesisTr())
                .storyThesis(request.storyThesis())
                .build();
        season.addEpisode(episode);
        applyEpisodeBlocks(episode, request.episodeBlocks());
        episode = episodeRepository.save(episode);
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
        episode.setTitleTr(request.titleTr());
        episode.setTitle(request.title());
        episode.setSynopsisTr(request.synopsisTr());
        episode.setSynopsis(request.synopsis());
        episode.setAirDate(request.airDate());
        episode.setDurationMinutes(request.durationMinutes());
        episode.setStillImageUrl(request.stillImageUrl());
        episode.setExternalRatingUpdatedAt(resolveExternalRatingUpdatedAt(request, episode));
        episode.setExternalRating(request.externalRating());
        episode.setExternalVoteCount(request.externalVoteCount());
        episode.setImdbId(request.imdbId());
        episode.setTmdbId(request.tmdbId());
        episode.setStoryKickerTr(request.storyKickerTr());
        episode.setStoryKicker(request.storyKicker());
        episode.setStoryTitleTr(request.storyTitleTr());
        episode.setStoryTitle(request.storyTitle());
        episode.setStoryThesisTr(request.storyThesisTr());
        episode.setStoryThesis(request.storyThesis());
        applyEpisodeBlocks(episode, request.episodeBlocks());
        return episodeMapper.toResponse(episode);
    }

    // requests null ise (generic scalar-only PUT gibi) mevcut bloklar KORUNUR —
    // sadece explicit bos liste ([]) gonderilirse tum bloklar silinir.
    private void applyEpisodeBlocks(Episode episode, List<EpisodeBlockRequest> requests) {
        if (requests == null) {
            return;
        }
        episode.clearEpisodeBlocks();
        int orderIndex = 0;
        for (EpisodeBlockRequest request : requests) {
            EpisodeBlock block = EpisodeBlock.builder()
                    .blockType(request.blockType())
                    .sceneKey(request.sceneKey())
                    .tone(request.tone())
                    .pinned(request.pinned())
                    .sceneKickerTr(request.sceneKickerTr())
                    .sceneKicker(request.sceneKicker())
                    .contentTr(request.contentTr())
                    .content(request.content())
                    .lead(request.lead())
                    .mediaUrl(request.mediaUrl())
                    .mediaAltTr(request.mediaAltTr())
                    .mediaAlt(request.mediaAlt())
                    .mediaRatio(request.mediaRatio())
                    .build();
            block.setOrderIndex(orderIndex++);
            block.setCol(request.col());
            block.setRow(request.row());
            episode.addEpisodeBlock(block);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Episode episode = findEntityById(id);
        imageStorageService.delete(episode.getStillImageUrl());
        episode.getSeason().removeEpisode(episode);
    }

    @Override
    public boolean existsById(Long id) {
        return episodeRepository.existsById(id);
    }

    private Episode findEntityById(Long id) {
        return episodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Episode bulunamadı: id=" + id));
    }

    // Client externalRatingUpdatedAt'i acikca gonderirse oncelikli; gonderilmezse
    // mevcut "rating degisince otomatik damgala" davranisi korunur (existing==null
    // create anlamina gelir).
    private LocalDateTime resolveExternalRatingUpdatedAt(EpisodeRequest request, Episode existing) {
        if (request.externalRatingUpdatedAt() != null) {
            return request.externalRatingUpdatedAt();
        }
        if (existing == null || !Objects.equals(existing.getExternalRating(), request.externalRating())) {
            return request.externalRating() != null ? LocalDateTime.now() : null;
        }
        return existing.getExternalRatingUpdatedAt();
    }
}
