package com.example.fandoom_backend.series.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.common.util.SlugGenerator;
import com.example.fandoom_backend.franchise.service.FranchiseService;
import com.example.fandoom_backend.genre.service.GenreService;
import com.example.fandoom_backend.series.dto.SeriesDetailResponse;
import com.example.fandoom_backend.series.dto.SeriesHeroBlockRequest;
import com.example.fandoom_backend.series.dto.SeriesHeroBlockResponse;
import com.example.fandoom_backend.series.dto.SeriesRequest;
import com.example.fandoom_backend.series.dto.SeriesSummaryResponse;
import com.example.fandoom_backend.series.entity.Series;
import com.example.fandoom_backend.series.entity.SeriesHeroBlock;
import com.example.fandoom_backend.series.mapper.SeriesHeroBlockMapper;
import com.example.fandoom_backend.series.mapper.SeriesMapper;
import com.example.fandoom_backend.series.repository.SeriesRepository;
import com.example.fandoom_backend.media.service.ImageStorageService;
import com.example.fandoom_backend.person.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeriesServiceImpl implements SeriesService {

    private final SeriesRepository seriesRepository;
    private final SeriesMapper seriesMapper;
    private final SeriesHeroBlockMapper seriesHeroBlockMapper;
    private final FranchiseService franchiseService;
    private final GenreService genreService;
    private final PersonService personService;
    private final ImageStorageService imageStorageService;

    @Override
    // Katalog listesi sik okunur, yazmada evict edilir
    @Cacheable(cacheNames = SeriesCacheNames.LIST, key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort", sync = true, condition = "#pageable.pageNumber < 5")
    public PageResponse<SeriesSummaryResponse> list(Pageable pageable) {
        Page<SeriesSummaryResponse> page = seriesRepository.findAll(pageable).map(seriesMapper::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Override
    // Franchise bazli liste
    @Cacheable(cacheNames = SeriesCacheNames.LIST, key = "'franchise:' + #franchiseId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort", sync = true, condition = "#pageable.pageNumber < 5")
    public PageResponse<SeriesSummaryResponse> listByFranchise(Long franchiseId, Pageable pageable) {
        Page<SeriesSummaryResponse> page = seriesRepository.findByFranchiseId(franchiseId, pageable)
                .map(seriesMapper::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Override
    // Genre bazli liste
    @Cacheable(cacheNames = SeriesCacheNames.LIST, key = "'genre:' + #genreId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort", sync = true, condition = "#pageable.pageNumber < 5")
    public PageResponse<SeriesSummaryResponse> listByGenre(Long genreId, Pageable pageable) {
        Page<SeriesSummaryResponse> page = seriesRepository.findByGenreIdsContains(genreId, pageable)
                .map(seriesMapper::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Override
    public PageResponse<SeriesSummaryResponse> search(String query, Pageable pageable) {
        Page<SeriesSummaryResponse> page = seriesRepository
                .searchByTitle(query, pageable)
                .map(seriesMapper::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Override
    // Detay sayfasi sik okunur
    @Cacheable(cacheNames = SeriesCacheNames.DETAIL, key = "'id:' + #id", sync = true)
    public SeriesDetailResponse getById(Long id) {
        Series series = seriesRepository.findWithSeasonsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Series bulunamadı: id=" + id));
        return seriesMapper.toDetailResponse(series);
    }

    @Override
    // Detay sayfasi sik okunur
    @Cacheable(cacheNames = SeriesCacheNames.DETAIL, key = "'slug:' + #slug", sync = true)
    public SeriesDetailResponse getBySlug(String slug) {
        Series series = seriesRepository.findWithSeasonsBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Series bulunamadı: slug=" + slug));
        return seriesMapper.toDetailResponse(series);
    }

    @Override
    @Transactional
    // Ayni kayit id ve slug key'iyle ayri cache'lendigi icin tum girdiler temizlenir
    @CacheEvict(cacheNames = {SeriesCacheNames.DETAIL, SeriesCacheNames.LIST, SeriesCacheNames.HERO}, allEntries = true)
    public SeriesDetailResponse create(SeriesRequest request) {
        validateReferences(request.franchiseId(), request.genreIds(), request.producerIds());
        Series series = Series.builder()
                .titleTr(request.titleTr())
                .title(request.title())
                .originalTitle(request.originalTitle())
                .slug(SlugGenerator.generateUnique(request.title(), seriesRepository::existsBySlug))
                .synopsisTr(request.synopsisTr())
                .synopsis(request.synopsis())
                .firstAirDate(request.firstAirDate())
                .lastAirDate(request.lastAirDate())
                .status(request.status())
                .posterUrl(request.posterUrl())
                .coverImageUrl(request.coverImageUrl())
                .trailerUrl(request.trailerUrl())
                .contentRating(request.contentRating())
                .originCountry(request.originCountry())
                .originalLanguage(request.originalLanguage())
                .externalRating(request.externalRating())
                .externalVoteCount(request.externalVoteCount())
                .externalRatingUpdatedAt(resolveExternalRatingUpdatedAt(request, null))
                .imdbId(request.imdbId())
                .tmdbId(request.tmdbId())
                .franchiseId(request.franchiseId())
                .genreIds(request.genreIds() == null ? new HashSet<>() : new HashSet<>(request.genreIds()))
                .producerIds(request.producerIds() == null ? new HashSet<>() : new HashSet<>(request.producerIds()))
                .build();
        return seriesMapper.toDetailResponse(seriesRepository.save(series));
    }

    @Override
    @Transactional
    // Ayni kayit id ve slug key'iyle ayri cache'lendigi icin tum girdiler temizlenir
    @CacheEvict(cacheNames = {SeriesCacheNames.DETAIL, SeriesCacheNames.LIST, SeriesCacheNames.HERO}, allEntries = true)
    public List<SeriesDetailResponse> createBatch(List<SeriesRequest> requests) {
        return requests.stream()
                .map(this::create)
                .toList();
    }

    @Override
    @Transactional
    // Ayni kayit id ve slug key'iyle ayri cache'lendigi icin tum girdiler temizlenir
    @CacheEvict(cacheNames = {SeriesCacheNames.DETAIL, SeriesCacheNames.LIST, SeriesCacheNames.HERO}, allEntries = true)
    public SeriesDetailResponse update(Long id, SeriesRequest request) {
        validateReferences(request.franchiseId(), request.genreIds(), request.producerIds());
        Series series = findEntityById(id);
        imageStorageService.deleteIfChanged(series.getPosterUrl(), request.posterUrl());
        imageStorageService.deleteIfChanged(series.getCoverImageUrl(), request.coverImageUrl());
        if (!series.getTitle().equals(request.title())) {
            series.setSlug(SlugGenerator.generateUnique(request.title(),
                    slug -> seriesRepository.existsBySlugAndIdNot(slug, id)));
        }
        series.setTitleTr(request.titleTr());
        series.setTitle(request.title());
        series.setOriginalTitle(request.originalTitle());
        series.setSynopsisTr(request.synopsisTr());
        series.setSynopsis(request.synopsis());
        series.setFirstAirDate(request.firstAirDate());
        series.setLastAirDate(request.lastAirDate());
        series.setStatus(request.status());
        series.setPosterUrl(request.posterUrl());
        series.setCoverImageUrl(request.coverImageUrl());
        series.setTrailerUrl(request.trailerUrl());
        series.setContentRating(request.contentRating());
        series.setOriginCountry(request.originCountry());
        series.setOriginalLanguage(request.originalLanguage());
        series.setExternalRatingUpdatedAt(resolveExternalRatingUpdatedAt(request, series));
        series.setExternalRating(request.externalRating());
        series.setExternalVoteCount(request.externalVoteCount());
        series.setImdbId(request.imdbId());
        series.setTmdbId(request.tmdbId());
        series.setFranchiseId(request.franchiseId());
        if (request.genreIds() != null) {
            series.setGenreIds(new HashSet<>(request.genreIds()));
        }
        if (request.producerIds() != null) {
            series.setProducerIds(new HashSet<>(request.producerIds()));
        }
        return seriesMapper.toDetailResponse(series);
    }

    @Override
    @Transactional
    // Ayni kayit id ve slug key'iyle ayri cache'lendigi icin tum girdiler temizlenir
    @CacheEvict(cacheNames = {SeriesCacheNames.DETAIL, SeriesCacheNames.LIST, SeriesCacheNames.HERO}, allEntries = true)
    public void delete(Long id) {
        Series series = findEntityById(id);
        imageStorageService.delete(series.getPosterUrl());
        imageStorageService.delete(series.getCoverImageUrl());
        seriesRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return seriesRepository.existsById(id);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return seriesRepository.existsBySlug(slug);
    }

    @Override
    // Hero bloklari detay sayfasiyla birlikte sik okunur
    @Cacheable(cacheNames = SeriesCacheNames.HERO, key = "'id:' + #id", sync = true)
    public List<SeriesHeroBlockResponse> getHeroBlocks(Long id) {
        return seriesHeroBlockMapper.toResponseList(findEntityById(id).getHeroBlocks());
    }

    @Override
    @Transactional
    // Ayni kayit id ve slug key'iyle ayri cache'lendigi icin tum girdiler temizlenir
    @CacheEvict(cacheNames = {SeriesCacheNames.DETAIL, SeriesCacheNames.LIST, SeriesCacheNames.HERO}, allEntries = true)
    public List<SeriesHeroBlockResponse> replaceHeroBlocks(Long id, List<SeriesHeroBlockRequest> requests) {
        Series series = findEntityById(id);
        series.clearHeroBlocks();
        int orderIndex = 0;
        for (SeriesHeroBlockRequest request : requests) {
            SeriesHeroBlock block = SeriesHeroBlock.builder()
                    .blockType(request.blockType())
                    .x(request.x())
                    .y(request.y())
                    .width(request.width())
                    .height(request.height())
                    .imageUrl(request.imageUrl())
                    .blurAmount(request.blurAmount())
                    .textTr(request.textTr())
                    .text(request.text())
                    .backgroundColor(request.backgroundColor())
                    .borderRadius(request.borderRadius())
                    .fontFamily(request.fontFamily())
                    .fontScale(request.fontScale() != null ? request.fontScale() : 1.0)
                    .build();
            block.setOrderIndex(orderIndex++);
            series.addHeroBlock(block);
        }
        return seriesHeroBlockMapper.toResponseList(series.getHeroBlocks());
    }

    private void validateReferences(Long franchiseId, Set<Long> genreIds, Set<Long> producerIds) {
        if (franchiseId != null && !franchiseService.existsById(franchiseId)) {
            throw new InvalidReferenceException("Geçersiz franchise id: " + franchiseId);
        }
        genreService.assertAllExist(genreIds);
        personService.assertAllExist(producerIds);
    }

    private Series findEntityById(Long id) {
        return seriesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Series bulunamadı: id=" + id));
    }

    // Client externalRatingUpdatedAt'i acikca gonderirse oncelikli; gonderilmezse
    // mevcut "rating degisince otomatik damgala" davranisi korunur (existing==null
    // create anlamina gelir).
    private LocalDateTime resolveExternalRatingUpdatedAt(SeriesRequest request, Series existing) {
        if (request.externalRatingUpdatedAt() != null) {
            return request.externalRatingUpdatedAt();
        }
        if (existing == null || !Objects.equals(existing.getExternalRating(), request.externalRating())) {
            return request.externalRating() != null ? LocalDateTime.now() : null;
        }
        return existing.getExternalRatingUpdatedAt();
    }
}
