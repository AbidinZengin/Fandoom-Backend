package com.example.fandoom_backend.person.repository;

import com.example.fandoom_backend.person.entity.Cast;
import com.example.fandoom_backend.person.entity.SubjectType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CastRepository extends JpaRepository<Cast, Long> {
    List<Cast> findBySubjectTypeAndSubjectIdOrderByBillingOrderAsc(SubjectType subjectType, Long subjectId);
}
