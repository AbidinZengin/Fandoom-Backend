package com.example.fandoom_backend.movie.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.common.util.SlugGenerator;
import com.example.fandoom_backend.franchise.service.FranchiseService;
import com.example.fandoom_backend.genre.service.GenreService;
import com.example.fandoom_backend.movie.dto.MovieDetailResponse;
import com.example.fandoom_backend.movie.dto.MovieRequest;
import com.example.fandoom_backend.movie.dto.MovieSummaryResponse;
import com.example.fandoom_backend.movie.entity.Movie;
import com.example.fandoom_backend.movie.entity.MovieStatus;
import com.example.fandoom_backend.movie.mapper.MovieMapper;
import com.example.fandoom_backend.movie.repository.MovieRepository;
import com.example.fandoom_backend.media.service.ImageStorageService;
import com.example.fandoom_backend.person.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final FranchiseService franchiseService;
    private final GenreService genreService;
    private final PersonService personService;
    private final ImageStorageService imageStorageService;

    @Override
    // Katalog listesi sik okunur, yazmada evict edilir
    @Cacheable(cacheNames = MovieCacheNames.LIST, key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort", sync = true, condition = "#pageable.pageNumber < 5")
    public PageResponse<MovieSummaryResponse> list(Pageable pageable) {
        Page<MovieSummaryResponse> page = movieRepository.findAll(pageable).map(movieMapper::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Override
    // Franchise bazli liste
    @Cacheable(cacheNames = MovieCacheNames.LIST, key = "'franchise:' + #franchiseId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort", sync = true, condition = "#pageable.pageNumber < 5")
    public PageResponse<MovieSummaryResponse> listByFranchise(Long franchiseId, Pageable pageable) {
        Page<MovieSummaryResponse> page = movieRepository.findByFranchiseId(franchiseId, pageable)
                .map(movieMapper::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Override
    // Genre bazli liste
    @Cacheable(cacheNames = MovieCacheNames.LIST, key = "'genre:' + #genreId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort", sync = true, condition = "#pageable.pageNumber < 5")
    public PageResponse<MovieSummaryResponse> listByGenre(Long genreId, Pageable pageable) {
        Page<MovieSummaryResponse> page = movieRepository.findByGenreIdsContains(genreId, pageable)
                .map(movieMapper::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Override
    public PageResponse<MovieSummaryResponse> search(String query, Pageable pageable) {
        Page<MovieSummaryResponse> page = movieRepository
                .searchByTitle(query, pageable)
                .map(movieMapper::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Override
    // Detay sayfasi sik okunur
    @Cacheable(cacheNames = MovieCacheNames.DETAIL, key = "'id:' + #id", sync = true)
    public MovieDetailResponse getById(Long id) {
        return movieMapper.toDetailResponse(findEntityById(id));
    }

    @Override
    // Detay sayfasi sik okunur
    @Cacheable(cacheNames = MovieCacheNames.DETAIL, key = "'slug:' + #slug", sync = true)
    public MovieDetailResponse getBySlug(String slug) {
        Movie movie = movieRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Movie bulunamadı: slug=" + slug));
        return movieMapper.toDetailResponse(movie);
    }

    @Override
    @Transactional
    // Ayni kayit id ve slug key'iyle ayri cache'lendigi icin tum girdiler temizlenir
    @CacheEvict(cacheNames = {MovieCacheNames.DETAIL, MovieCacheNames.LIST}, allEntries = true)
    public MovieDetailResponse create(MovieRequest request) {
        validateReferences(request);
        Movie movie = Movie.builder()
                .titleTr(request.titleTr())
                .title(request.title())
                .originalTitle(request.originalTitle())
                .slug(SlugGenerator.generateUnique(request.title(), movieRepository::existsBySlug))
                .synopsisTr(request.synopsisTr())
                .synopsis(request.synopsis())
                .taglineTr(request.taglineTr())
                .tagline(request.tagline())
                .status(resolveStatus(request, null))
                .budget(zeroToNull(request.budget()))
                .boxOffice(zeroToNull(request.boxOffice()))
                .releaseDate(request.releaseDate())
                .runtimeMinutes(request.runtimeMinutes())
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
                .directorIds(request.directorIds() == null ? new HashSet<>() : new HashSet<>(request.directorIds()))
                .writerIds(request.writerIds() == null ? new HashSet<>() : new HashSet<>(request.writerIds()))
                .build();
        return movieMapper.toDetailResponse(movieRepository.save(movie));
    }

    @Override
    @Transactional
    // Ayni kayit id ve slug key'iyle ayri cache'lendigi icin tum girdiler temizlenir
    @CacheEvict(cacheNames = {MovieCacheNames.DETAIL, MovieCacheNames.LIST}, allEntries = true)
    public List<MovieDetailResponse> createBatch(List<MovieRequest> requests) {
        return requests.stream()
                .map(this::create)
                .toList();
    }

    @Override
    @Transactional
    // Ayni kayit id ve slug key'iyle ayri cache'lendigi icin tum girdiler temizlenir
    @CacheEvict(cacheNames = {MovieCacheNames.DETAIL, MovieCacheNames.LIST}, allEntries = true)
    public MovieDetailResponse update(Long id, MovieRequest request) {
        validateReferences(request);
        Movie movie = findEntityById(id);
        imageStorageService.deleteIfChanged(movie.getPosterUrl(), request.posterUrl());
        imageStorageService.deleteIfChanged(movie.getCoverImageUrl(), request.coverImageUrl());
        if (!movie.getTitle().equals(request.title())) {
            movie.setSlug(SlugGenerator.generateUnique(request.title(),
                    slug -> movieRepository.existsBySlugAndIdNot(slug, id)));
        }
        movie.setTitleTr(request.titleTr());
        movie.setTitle(request.title());
        movie.setOriginalTitle(request.originalTitle());
        movie.setSynopsisTr(request.synopsisTr());
        movie.setSynopsis(request.synopsis());
        movie.setTaglineTr(request.taglineTr());
        movie.setTagline(request.tagline());
        movie.setStatus(resolveStatus(request, movie));
        movie.setBudget(zeroToNull(request.budget()));
        movie.setBoxOffice(zeroToNull(request.boxOffice()));
        movie.setReleaseDate(request.releaseDate());
        movie.setRuntimeMinutes(request.runtimeMinutes());
        movie.setPosterUrl(request.posterUrl());
        movie.setCoverImageUrl(request.coverImageUrl());
        movie.setTrailerUrl(request.trailerUrl());
        movie.setContentRating(request.contentRating());
        movie.setOriginCountry(request.originCountry());
        movie.setOriginalLanguage(request.originalLanguage());
        movie.setExternalRatingUpdatedAt(resolveExternalRatingUpdatedAt(request, movie));
        movie.setExternalRating(request.externalRating());
        movie.setExternalVoteCount(request.externalVoteCount());
        movie.setImdbId(request.imdbId());
        movie.setTmdbId(request.tmdbId());
        movie.setFranchiseId(request.franchiseId());
        if (request.genreIds() != null) {
            movie.setGenreIds(new HashSet<>(request.genreIds()));
        }
        if (request.producerIds() != null) {
            movie.setProducerIds(new HashSet<>(request.producerIds()));
        }
        if (request.directorIds() != null) {
            movie.setDirectorIds(new HashSet<>(request.directorIds()));
        }
        if (request.writerIds() != null) {
            movie.setWriterIds(new HashSet<>(request.writerIds()));
        }
        return movieMapper.toDetailResponse(movie);
    }

    @Override
    @Transactional
    // Ayni kayit id ve slug key'iyle ayri cache'lendigi icin tum girdiler temizlenir
    @CacheEvict(cacheNames = {MovieCacheNames.DETAIL, MovieCacheNames.LIST}, allEntries = true)
    public void delete(Long id) {
        Movie movie = findEntityById(id);
        imageStorageService.delete(movie.getPosterUrl());
        imageStorageService.delete(movie.getCoverImageUrl());
        movieRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return movieRepository.existsById(id);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return movieRepository.existsBySlug(slug);
    }

    private void validateReferences(MovieRequest request) {
        Long franchiseId = request.franchiseId();
        if (franchiseId != null && !franchiseService.existsById(franchiseId)) {
            throw new InvalidReferenceException("Geçersiz franchise id: " + franchiseId);
        }
        genreService.assertAllExist(request.genreIds());
        personService.assertAllExist(request.producerIds());
        personService.assertAllExist(request.directorIds());
        personService.assertAllExist(request.writerIds());
    }

    private static Long zeroToNull(Long amount) {
        return amount == null || amount == 0L ? null : amount;
    }

    // Client status göndermezse: update'te mevcut değer korunur (yoksa türetilir),
    // create'te releaseDate'ten türetilir — geçmiş/bugün → RELEASED, gelecek/boş → ANNOUNCED.
    private MovieStatus resolveStatus(MovieRequest request, Movie existing) {
        if (request.status() != null) {
            return request.status();
        }
        if (existing != null && existing.getStatus() != null) {
            return existing.getStatus();
        }
        LocalDate releaseDate = request.releaseDate();
        return releaseDate != null && !releaseDate.isAfter(LocalDate.now())
                ? MovieStatus.RELEASED
                : MovieStatus.ANNOUNCED;
    }

    private Movie findEntityById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie bulunamadı: id=" + id));
    }

    // Client externalRatingUpdatedAt'i acikca gonderirse oncelikli; gonderilmezse
    // mevcut "rating degisince otomatik damgala" davranisi korunur (existing==null
    // create anlamina gelir).
    private LocalDateTime resolveExternalRatingUpdatedAt(MovieRequest request, Movie existing) {
        if (request.externalRatingUpdatedAt() != null) {
            return request.externalRatingUpdatedAt();
        }
        if (existing == null || !Objects.equals(existing.getExternalRating(), request.externalRating())) {
            return request.externalRating() != null ? LocalDateTime.now() : null;
        }
        return existing.getExternalRatingUpdatedAt();
    }
}
