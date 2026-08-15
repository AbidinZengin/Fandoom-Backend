package com.example.fandoom_backend.lore.repository;

import com.example.fandoom_backend.lore.entity.Event;
import com.example.fandoom_backend.lore.entity.SubjectType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findBySubjectTypeAndSubjectIdOrderByOrderIndexAsc(SubjectType subjectType, Long subjectId);
}
