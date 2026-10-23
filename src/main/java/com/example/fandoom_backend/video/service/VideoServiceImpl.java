package com.example.fandoom_backend.video.service;

import com.example.fandoom_backend.common.exception.DuplicateResourceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.service.SeriesService;
import com.example.fandoom_backend.video.dto.VideoCreateRequest;
import com.example.fandoom_backend.video.dto.VideoResponse;
import com.example.fandoom_backend.video.dto.VideoUpdateRequest;
import com.example.fandoom_backend.video.entity.Video;
import com.example.fandoom_backend.video.entity.VideoItemType;
import com.example.fandoom_backend.video.mapper.VideoMapper;
import com.example.fandoom_backend.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoServiceImpl implements VideoService {

    private final VideoRepository videoRepository;
    private final VideoMapper videoMapper;
    private final MovieService movieService;
    private final SeriesService seriesService;

    @Override
    public List<VideoResponse> listByProduction(VideoItemType itemType, String slug) {
        Long itemId = resolveItemId(itemType, slug);
        return videoMapper.toResponseList(
                videoRepository.findByItemTypeAndItemIdOrderBySortOrderAscIdAsc(itemType, itemId));
    }

    @Override
    @Transactional
    public VideoResponse create(VideoItemType itemType, String slug, VideoCreateRequest request) {
        Long itemId = resolveItemId(itemType, slug);
        if (videoRepository.existsByItemTypeAndItemIdAndYoutubeId(itemType, itemId, request.youtubeId())) {
            throw new DuplicateResourceException("Bu video yapıma zaten eklenmiş: " + request.youtubeId());
        }
        Video video = Video.builder()
                .itemId(itemId)
                .itemType(itemType)
                .youtubeId(request.youtubeId())
                .title(request.title().trim())
                .channelName(request.channelName().trim())
                .channelUrl(request.channelUrl().trim())
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : nextSortOrder(itemType, itemId))
                .build();
        return videoMapper.toResponse(videoRepository.save(video));
    }

    @Override
    @Transactional
    public VideoResponse update(Long id, VideoUpdateRequest request) {
        Video video = findOrThrow(id);
        video.setTitle(request.title().trim());
        video.setChannelName(request.channelName().trim());
        video.setChannelUrl(request.channelUrl().trim());
        if (request.sortOrder() != null) {
            video.setSortOrder(request.sortOrder());
        }
        return videoMapper.toResponse(video);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        videoRepository.delete(findOrThrow(id));
    }

    private Video findOrThrow(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Video bulunamadı: id=" + id));
    }

    private int nextSortOrder(VideoItemType itemType, Long itemId) {
        Integer max = videoRepository.findMaxSortOrder(itemType, itemId);
        return max == null ? 0 : max + 1;
    }

    // Slug → id: getBySlug yapım yoksa zaten ResourceNotFoundException (404) fırlatır.
    private Long resolveItemId(VideoItemType itemType, String slug) {
        return switch (itemType) {
            case MOVIE -> movieService.getBySlug(slug).id();
            case SERIES -> seriesService.getBySlug(slug).id();
        };
    }
}
