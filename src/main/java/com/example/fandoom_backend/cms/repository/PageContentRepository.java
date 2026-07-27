package com.example.fandoom_backend.cms.repository;

import com.example.fandoom_backend.cms.entity.PageContent;
import com.example.fandoom_backend.cms.entity.PageName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PageContentRepository extends JpaRepository<PageContent, Long> {
    List<PageContent> findByPageAndEntityIdAndActiveTrueOrderByOrderIndexAsc(PageName page, Long entityId);
}
