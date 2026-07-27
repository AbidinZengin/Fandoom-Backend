package com.example.fandoom_backend.production.service;

import com.example.fandoom_backend.common.dto.CursorPageResponse;
import com.example.fandoom_backend.movie.entity.Movie;
import com.example.fandoom_backend.movie.repository.MovieRepository;
import com.example.fandoom_backend.production.dto.ProductionSummaryResponse;
import com.example.fandoom_backend.production.dto.ProductionType;
import com.example.fandoom_backend.series.entity.Series;
import com.example.fandoom_backend.series.repository.SeriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductionServiceImpl implements ProductionService {

    private final MovieRepository movieRepository;
    private final SeriesRepository seriesRepository;

    private static final Comparator<ProductionSummaryResponse> DESC_BY_DATE_THEN_ID =
            Comparator.comparing(ProductionSummaryResponse::releaseDate,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Comparator.comparing(ProductionSummaryResponse::id).reversed());

    @Override
    public CursorPageResponse<ProductionSummaryResponse> list(LocalDate cursorDate, Long cursorId, int size) {
        int fetchSize = size + 1;

        List<Movie> movies = movieRepository.findPageBeforeCursor(cursorDate, cursorId, PageRequest.of(0, fetchSize));
        List<Series> series = seriesRepository.findPageBeforeCursor(cursorDate, cursorId, PageRequest.of(0, fetchSize));

        boolean moviesHasMore = movies.size() > size;
        boolean seriesHasMore = series.size() > size;
        List<Movie> trimmedMovies = moviesHasMore ? movies.subList(0, size) : movies;
        List<Series> trimmedSeries = seriesHasMore ? series.subList(0, size) : series;

        List<ProductionSummaryResponse> pool = new ArrayList<>();
        trimmedMovies.forEach(m -> pool.add(new ProductionSummaryResponse(
                m.getId(), m.getSlug(), m.getTitle(), ProductionType.MOVIE, m.getPosterUrl(), m.getReleaseDate())));
        trimmedSeries.forEach(s -> pool.add(new ProductionSummaryResponse(
                s.getId(), s.getSlug(), s.getTitle(), ProductionType.SERIES, s.getPosterUrl(), s.getFirstAirDate())));
        pool.sort(DESC_BY_DATE_THEN_ID);

        boolean poolHasMore = pool.size() > size;
        List<ProductionSummaryResponse> windowed = poolHasMore ? pool.subList(0, size) : pool;

        boolean hasNext = moviesHasMore || seriesHasMore || poolHasMore;
        ProductionSummaryResponse last = windowed.isEmpty() ? null : windowed.get(windowed.size() - 1);
        LocalDate nextCursorDate = hasNext && last != null ? last.releaseDate() : null;
        Long nextCursorId = hasNext && last != null ? last.id() : null;

        return new CursorPageResponse<>(windowed, hasNext, nextCursorDate, nextCursorId);
    }
}
