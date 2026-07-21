package com.example.fandoom_backend.genre.repository;

import com.example.fandoom_backend.genre.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre, Long> {
    Optional<Genre> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByName(String name);
}
