package com.example.fandoom_backend.series.repository;

import com.example.fandoom_backend.series.entity.Series;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SeriesRepository extends JpaRepository<Series, Long> {
    Optional<Series> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    Page<Series> findByFranchiseId(Long franchiseId, Pageable pageable);
    Page<Series> findByGenreIdsContains(Long genreId, Pageable pageable);
    Page<Series> findByTitleContainingIgnoreCaseOrOriginalTitleContainingIgnoreCase(
            String title, String originalTitle, Pageable pageable);

    @Query("SELECT s FROM Series s WHERE :cursorDate IS NULL "
            + "OR s.firstAirDate < :cursorDate "
            + "OR (s.firstAirDate = :cursorDate AND s.id < :cursorId) "
            + "ORDER BY s.firstAirDate DESC, s.id DESC")
    List<Series> findPageBeforeCursor(
            @Param("cursorDate") LocalDate cursorDate, @Param("cursorId") Long cursorId, Pageable pageable);
}
