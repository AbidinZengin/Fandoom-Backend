package com.example.fandoom_backend.group.repository;

import com.example.fandoom_backend.group.entity.GroupAssignment;
import com.example.fandoom_backend.group.entity.TaggableType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupAssignmentRepository extends JpaRepository<GroupAssignment, Long> {
    List<GroupAssignment> findByTaggableTypeAndTaggableId(TaggableType taggableType, Long taggableId);
    boolean existsByGroupIdAndTaggableTypeAndTaggableId(Long groupId, TaggableType taggableType, Long taggableId);
}
