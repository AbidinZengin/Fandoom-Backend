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
import com.example.fandoom_backend.movie.mapper.MovieMapper;
import com.example.fandoom_backend.movie.repository.MovieRepository;
import com.example.fandoom_backend.media.service.ImageStorageService;
import com.example.fandoom_backend.person.service.PersonService;
import lombok.RequiredArgsConstructor;
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
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final FranchiseService franchiseService;
    private final GenreService genreService;
    private final PersonService personService;
    private final ImageStorageService imageStorageService;

    @Override
    public PageResponse<MovieSummaryResponse> list(Pageable pageable) {
        Page<MovieSummaryResponse> page = movieRepository.findAll(pageable).map(movieMapper::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Override
    public PageResponse<MovieSummaryResponse> listByFranchise(Long franchiseId, Pageable pageable) {
        Page<MovieSummaryResponse> page = movieRepository.findByFranchiseId(franchiseId, pageable)
                .map(movieMapper::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Override
    public PageResponse<MovieSummaryResponse> listByGenre(Long genreId, Pageable pageable) {
        Page<MovieSummaryResponse> page = movieRepository.findByGenreIdsContains(genreId, pageable)
                .map(movieMapper::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Override
    public PageResponse<MovieSummaryResponse> search(String query, Pageable pageable) {
        Page<MovieSummaryResponse> page = movieRepository
                .findByTitleContainingIgnoreCaseOrOriginalTitleContainingIgnoreCase(query, query, pageable)
                .map(movieMapper::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Override
    public MovieDetailResponse getById(Long id) {
        return movieMapper.toDetailResponse(findEntityById(id));
    }

    @Override
    public MovieDetailResponse getBySlug(String slug) {
        Movie movie = movieRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Movie bulunamadı: slug=" + slug));
        return movieMapper.toDetailResponse(movie);
    }

    @Override
    @Transactional
    public MovieDetailResponse create(MovieRequest request) {
        validateReferences(request.franchiseId(), request.genreIds(), request.producerIds());
        Movie movie = Movie.builder()
                .title(request.title())
                .originalTitle(request.originalTitle())
                .slug(SlugGenerator.generateUnique(request.title(), movieRepository::existsBySlug))
                .synopsis(request.synopsis())
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
                .externalRatingUpdatedAt(request.externalRating() != null ? LocalDateTime.now() : null)
                .imdbId(request.imdbId())
                .tmdbId(request.tmdbId())
                .franchiseId(request.franchiseId())
                .genreIds(request.genreIds() == null ? new HashSet<>() : new HashSet<>(request.genreIds()))
                .producerIds(request.producerIds() == null ? new HashSet<>() : new HashSet<>(request.producerIds()))
                .build();
        return movieMapper.toDetailResponse(movieRepository.save(movie));
    }

    @Override
    @Transactional
    public List<MovieDetailResponse> createBatch(List<MovieRequest> requests) {
        return requests.stream()
                .map(this::create)
                .toList();
    }

    @Override
    @Transactional
    public MovieDetailResponse update(Long id, MovieRequest request) {
        validateReferences(request.franchiseId(), request.genreIds(), request.producerIds());
        Movie movie = findEntityById(id);
        imageStorageService.deleteIfChanged(movie.getPosterUrl(), request.posterUrl());
        imageStorageService.deleteIfChanged(movie.getCoverImageUrl(), request.coverImageUrl());
        if (!movie.getTitle().equals(request.title())) {
            movie.setSlug(SlugGenerator.generateUnique(request.title(),
                    slug -> movieRepository.existsBySlugAndIdNot(slug, id)));
        }
        movie.setTitle(request.title());
        movie.setOriginalTitle(request.originalTitle());
        movie.setSynopsis(request.synopsis());
        movie.setReleaseDate(request.releaseDate());
        movie.setRuntimeMinutes(request.runtimeMinutes());
        movie.setPosterUrl(request.posterUrl());
        movie.setCoverImageUrl(request.coverImageUrl());
        movie.setTrailerUrl(request.trailerUrl());
        movie.setContentRating(request.contentRating());
        movie.setOriginCountry(request.originCountry());
        movie.setOriginalLanguage(request.originalLanguage());
        if (!Objects.equals(movie.getExternalRating(), request.externalRating())) {
            movie.setExternalRatingUpdatedAt(request.externalRating() != null ? LocalDateTime.now() : null);
        }
        movie.setExternalRating(request.externalRating());
        movie.setExternalVoteCount(request.externalVoteCount());
        movie.setImdbId(request.imdbId());
        movie.setTmdbId(request.tmdbId());
        movie.setFranchiseId(request.franchiseId());
        movie.setGenreIds(request.genreIds() == null ? new HashSet<>() : new HashSet<>(request.genreIds()));
        movie.setProducerIds(request.producerIds() == null ? new HashSet<>() : new HashSet<>(request.producerIds()));
        return movieMapper.toDetailResponse(movie);
    }

    @Override
    @Transactional
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

    private void validateReferences(Long franchiseId, Set<Long> genreIds, Set<Long> producerIds) {
        if (franchiseId != null && !franchiseService.existsById(franchiseId)) {
            throw new InvalidReferenceException("Geçersiz franchise id: " + franchiseId);
        }
        genreService.assertAllExist(genreIds);
        personService.assertAllExist(producerIds);
    }

    private Movie findEntityById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie bulunamadı: id=" + id));
    }
}
