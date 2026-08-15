package com.example.fandoom_backend.lore.repository;

import com.example.fandoom_backend.lore.entity.SubjectType;
import com.example.fandoom_backend.lore.entity.TaxonomyCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaxonomyCategoryRepository extends JpaRepository<TaxonomyCategory, Long> {
    Optional<TaxonomyCategory> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    List<TaxonomyCategory> findBySubjectTypeAndSubjectId(SubjectType subjectType, Long subjectId);
}
