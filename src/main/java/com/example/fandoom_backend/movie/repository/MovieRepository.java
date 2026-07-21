package com.example.fandoom_backend.movie.repository;

import com.example.fandoom_backend.movie.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Page<Movie> findByFranchiseId(Long franchiseId, Pageable pageable);
    Page<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
