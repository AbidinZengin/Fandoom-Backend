package com.example.fandoom_backend.tag.repository;

import com.example.fandoom_backend.tag.entity.TagAssignment;
import com.example.fandoom_backend.tag.entity.TaggableType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TagAssignmentRepository extends JpaRepository<TagAssignment, Long> {
    // TagAssignmentMapper her satırda tag.id/name/slug/type okuduğu için tag JOIN FETCH edilir
    // (aksi halde atama başına ayrı bir SELECT tag — N+1).
    @EntityGraph(attributePaths = "tag")
    List<TagAssignment> findByTaggableTypeAndTaggableId(TaggableType taggableType, Long taggableId);

    @EntityGraph(attributePaths = "tag")
    List<TagAssignment> findByTaggableTypeAndTaggableIdIn(TaggableType taggableType, Collection<Long> taggableIds);
    boolean existsByTagIdAndTaggableTypeAndTaggableId(Long tagId, TaggableType taggableType, Long taggableId);
}
