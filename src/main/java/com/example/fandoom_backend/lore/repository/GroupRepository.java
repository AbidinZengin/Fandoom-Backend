package com.example.fandoom_backend.lore.repository;

import com.example.fandoom_backend.lore.entity.Group;
import com.example.fandoom_backend.lore.entity.SubjectType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    List<Group> findByCategory_SubjectTypeAndCategory_SubjectId(SubjectType subjectType, Long subjectId);
    List<Group> findByCategory_SubjectTypeAndCategory_SubjectIdAndCategoryId(
            SubjectType subjectType, Long subjectId, Long categoryId);
}
