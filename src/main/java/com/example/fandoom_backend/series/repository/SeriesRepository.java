package com.example.fandoom_backend.series.repository;

import com.example.fandoom_backend.series.entity.Series;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeriesRepository extends JpaRepository<Series, Long> {
    Optional<Series> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Page<Series> findByFranchiseId(Long franchiseId, Pageable pageable);
    Page<Series> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
