package com.example.fandoom_backend.tag.repository;

import com.example.fandoom_backend.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}
