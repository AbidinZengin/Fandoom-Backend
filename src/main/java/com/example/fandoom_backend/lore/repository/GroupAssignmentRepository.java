package com.example.fandoom_backend.lore.repository;

import com.example.fandoom_backend.lore.entity.GroupAssignment;
import com.example.fandoom_backend.lore.entity.TaggableType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupAssignmentRepository extends JpaRepository<GroupAssignment, Long> {
    List<GroupAssignment> findByTaggableTypeAndTaggableId(TaggableType taggableType, Long taggableId);
    boolean existsByGroupIdAndTaggableTypeAndTaggableId(Long groupId, TaggableType taggableType, Long taggableId);
}
