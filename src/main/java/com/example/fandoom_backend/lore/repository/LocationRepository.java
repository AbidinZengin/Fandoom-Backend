package com.example.fandoom_backend.lore.repository;

import com.example.fandoom_backend.lore.entity.Location;
import com.example.fandoom_backend.lore.entity.SubjectType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    List<Location> findBySubjectTypeAndSubjectId(SubjectType subjectType, Long subjectId);
}
