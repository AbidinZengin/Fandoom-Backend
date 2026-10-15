package com.example.fandoom_backend.movie.repository;

import com.example.fandoom_backend.common.util.LikePatterns;
import com.example.fandoom_backend.movie.dto.MovieSearchResult;
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
    @Query("SELECT m FROM Movie m WHERE "
            + "LOWER(m.titleTr) LIKE LOWER(CONCAT('%', :query, '%')) "
            + "OR LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%')) "
            + "OR LOWER(m.originalTitle) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Movie> searchByTitle(@Param("query") String query, Pageable pageable);

    // Quick search: 3 başlıktan birinde geçenler; önce "ile başlayanlar" (0), sonra "içinde geçenler" (1),
    // eşitlikte title, sonra id (deterministik sıra). Kalıplar LikePatterns'tan gelir. List dönüşü = COUNT sorgusu yok.
    // LOWER() yok: kolon collation'ı (utf8mb4_unicode_ci) zaten harfe duyarsız.
    @Query("SELECT new com.example.fandoom_backend.movie.dto.MovieSearchResult("
            + "m.id, m.slug, m.title, m.titleTr, m.posterUrl, m.releaseDate) FROM Movie m "
            + "WHERE m.title LIKE :contains" + LikePatterns.ESCAPE
            + " OR m.titleTr LIKE :contains" + LikePatterns.ESCAPE
            + " OR m.originalTitle LIKE :contains" + LikePatterns.ESCAPE
            + " ORDER BY CASE WHEN m.title LIKE :prefix" + LikePatterns.ESCAPE
            + " OR m.titleTr LIKE :prefix" + LikePatterns.ESCAPE
            + " OR m.originalTitle LIKE :prefix" + LikePatterns.ESCAPE
            + " THEN 0 ELSE 1 END, m.title ASC, m.id ASC")
    List<MovieSearchResult> quickSearch(@Param("contains") String containsPattern,
                                        @Param("prefix") String prefixPattern, Pageable pageable);

    @Query("SELECT m FROM Movie m WHERE :cursorDate IS NULL "
            + "OR m.releaseDate < :cursorDate "
            + "OR (m.releaseDate = :cursorDate AND m.id < :cursorId) "
            + "ORDER BY m.releaseDate DESC, m.id DESC")
    List<Movie> findPageBeforeCursor(
            @Param("cursorDate") LocalDate cursorDate, @Param("cursorId") Long cursorId, Pageable pageable);
}
