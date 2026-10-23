package com.example.fandoom_backend.video.repository;

import com.example.fandoom_backend.video.entity.Video;
import com.example.fandoom_backend.video.entity.VideoItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {

    List<Video> findByItemTypeAndItemIdOrderBySortOrderAscIdAsc(VideoItemType itemType, Long itemId);

    boolean existsByItemTypeAndItemIdAndYoutubeId(VideoItemType itemType, Long itemId, String youtubeId);

    // Yapımda hiç video yoksa null.
    @Query("select max(v.sortOrder) from Video v where v.itemType = :itemType and v.itemId = :itemId")
    Integer findMaxSortOrder(@Param("itemType") VideoItemType itemType, @Param("itemId") Long itemId);
}
