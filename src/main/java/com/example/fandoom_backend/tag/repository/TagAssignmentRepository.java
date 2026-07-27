package com.example.fandoom_backend.tag.repository;

import com.example.fandoom_backend.tag.entity.TagAssignment;
import com.example.fandoom_backend.tag.entity.TaggableType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TagAssignmentRepository extends JpaRepository<TagAssignment, Long> {
    List<TagAssignment> findByTaggableTypeAndTaggableId(TaggableType taggableType, Long taggableId);
    boolean existsByTagIdAndTaggableTypeAndTaggableId(Long tagId, TaggableType taggableType, Long taggableId);
}
