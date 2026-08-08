package com.example.fandoom_backend.cms.repository;

import com.example.fandoom_backend.cms.entity.HomeBlock;
import com.example.fandoom_backend.cms.entity.PageName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomeBlockRepository extends JpaRepository<HomeBlock, Long> {
    List<HomeBlock> findByPageAndEntityIdAndActiveTrueOrderByOrderIndexAsc(PageName page, Long entityId);
}
