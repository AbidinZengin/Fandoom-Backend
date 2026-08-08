package com.example.fandoom_backend.group.repository;

import com.example.fandoom_backend.group.entity.Group;
import com.example.fandoom_backend.group.entity.GroupType;
import com.example.fandoom_backend.group.entity.SubjectType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    List<Group> findBySubjectTypeAndSubjectId(SubjectType subjectType, Long subjectId);
    List<Group> findBySubjectTypeAndSubjectIdAndType(SubjectType subjectType, Long subjectId, GroupType type);
}
