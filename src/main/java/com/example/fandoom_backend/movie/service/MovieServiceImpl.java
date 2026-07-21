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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final FranchiseService franchiseService;
    private final GenreService genreService;
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
    public PageResponse<MovieSummaryResponse> search(String query, Pageable pageable) {
        Page<MovieSummaryResponse> page = movieRepository.findByTitleContainingIgnoreCase(query, pageable)
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
        validateReferences(request.franchiseId(), request.genreIds());
        Movie movie = Movie.builder()
                .title(request.title())
                .slug(SlugGenerator.generateUnique(request.title(), movieRepository::existsBySlug))
                .synopsis(request.synopsis())
                .releaseDate(request.releaseDate())
                .runtimeMinutes(request.runtimeMinutes())
                .posterUrl(request.posterUrl())
                .coverImageUrl(request.coverImageUrl())
                .franchiseId(request.franchiseId())
                .genreIds(request.genreIds() == null ? new HashSet<>() : new HashSet<>(request.genreIds()))
                .build();
        return movieMapper.toDetailResponse(movieRepository.save(movie));
    }

    @Override
    @Transactional
    public MovieDetailResponse update(Long id, MovieRequest request) {
        validateReferences(request.franchiseId(), request.genreIds());
        Movie movie = findEntityById(id);
        imageStorageService.deleteIfChanged(movie.getPosterUrl(), request.posterUrl());
        imageStorageService.deleteIfChanged(movie.getCoverImageUrl(), request.coverImageUrl());
        if (!movie.getTitle().equals(request.title())) {
            movie.setSlug(SlugGenerator.generateUnique(request.title(), movieRepository::existsBySlug));
        }
        movie.setTitle(request.title());
        movie.setSynopsis(request.synopsis());
        movie.setReleaseDate(request.releaseDate());
        movie.setRuntimeMinutes(request.runtimeMinutes());
        movie.setPosterUrl(request.posterUrl());
        movie.setCoverImageUrl(request.coverImageUrl());
        movie.setFranchiseId(request.franchiseId());
        movie.setGenreIds(request.genreIds() == null ? new HashSet<>() : new HashSet<>(request.genreIds()));
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

    private void validateReferences(Long franchiseId, Set<Long> genreIds) {
        if (franchiseId != null && !franchiseService.existsById(franchiseId)) {
            throw new InvalidReferenceException("Geçersiz franchise id: " + franchiseId);
        }
        genreService.assertAllExist(genreIds);
    }

    private Movie findEntityById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie bulunamadı: id=" + id));
    }
}
