package com.example.fandoom_backend.person.repository;

import com.example.fandoom_backend.person.entity.Character;
import com.example.fandoom_backend.person.entity.SubjectType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CharacterRepository extends JpaRepository<Character, Long> {
    Optional<Character> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    List<Character> findBySubjectTypeAndSubjectIdOrderByBillingOrderAsc(SubjectType subjectType, Long subjectId);
}
