package com.example.fandoom_backend.contentdrop.repository;

import com.example.fandoom_backend.contentdrop.entity.ContentDropRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentDropRelationRepository extends JpaRepository<ContentDropRelation, Long> {
    List<ContentDropRelation> findBySourceContentDropIdOrderByOrderIndexAsc(Long sourceContentDropId);
}
