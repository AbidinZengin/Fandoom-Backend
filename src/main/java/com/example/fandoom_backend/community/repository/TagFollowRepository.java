package com.example.fandoom_backend.community.repository;

import com.example.fandoom_backend.community.entity.TagFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TagFollowRepository extends JpaRepository<TagFollow, Long> {

    boolean existsByUserIdAndTag(Long userId, String tag);

    void deleteByUserIdAndTag(Long userId, String tag);

    List<TagFollow> findByUserId(Long userId);
}
