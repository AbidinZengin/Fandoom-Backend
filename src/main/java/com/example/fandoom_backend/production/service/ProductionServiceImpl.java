package com.example.fandoom_backend.production.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.movie.dto.MovieSummaryResponse;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.production.dto.ProductionSummaryResponse;
import com.example.fandoom_backend.production.dto.ProductionType;
import com.example.fandoom_backend.series.dto.SeriesSummaryResponse;
import com.example.fandoom_backend.series.service.SeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductionServiceImpl implements ProductionService {

    private final MovieService movieService;
    private final SeriesService seriesService;

    @Override
    public PageResponse<ProductionSummaryResponse> list(Pageable pageable) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int fetchSize = (page + 1) * size;

        PageResponse<MovieSummaryResponse> movies = movieService.list(
                PageRequest.of(0, fetchSize, Sort.by(Sort.Direction.DESC, "releaseDate")));
        PageResponse<SeriesSummaryResponse> series = seriesService.list(
                PageRequest.of(0, fetchSize, Sort.by(Sort.Direction.DESC, "firstAirDate")));

        List<ProductionSummaryResponse> merged = new ArrayList<>();
        movies.content().forEach(m -> merged.add(new ProductionSummaryResponse(
                m.id(), m.slug(), m.title(), ProductionType.MOVIE, m.posterUrl(), m.releaseDate())));
        series.content().forEach(s -> merged.add(new ProductionSummaryResponse(
                s.id(), s.slug(), s.title(), ProductionType.SERIES, s.posterUrl(), s.firstAirDate())));
        merged.sort(Comparator.comparing(ProductionSummaryResponse::releaseDate,
                Comparator.nullsLast(Comparator.reverseOrder())));

        long totalElements = movies.totalElements() + series.totalElements();
        int fromIndex = Math.min(page * size, merged.size());
        int toIndex = Math.min(fromIndex + size, merged.size());
        List<ProductionSummaryResponse> windowed = merged.subList(fromIndex, toIndex);

        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        boolean last = (page + 1) >= totalPages;

        return new PageResponse<>(windowed, page, size, totalElements, totalPages, last);
    }
}
