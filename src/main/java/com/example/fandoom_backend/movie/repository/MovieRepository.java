package com.example.fandoom_backend.movie.repository;

import com.example.fandoom_backend.movie.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    Page<Movie> findByFranchiseId(Long franchiseId, Pageable pageable);
    Page<Movie> findByGenreIdsContains(Long genreId, Pageable pageable);
    Page<Movie> findByTitleContainingIgnoreCaseOrOriginalTitleContainingIgnoreCase(
            String title, String originalTitle, Pageable pageable);

    @Query("SELECT m FROM Movie m WHERE :cursorDate IS NULL "
            + "OR m.releaseDate < :cursorDate "
            + "OR (m.releaseDate = :cursorDate AND m.id < :cursorId) "
            + "ORDER BY m.releaseDate DESC, m.id DESC")
    List<Movie> findPageBeforeCursor(
            @Param("cursorDate") LocalDate cursorDate, @Param("cursorId") Long cursorId, Pageable pageable);
}
