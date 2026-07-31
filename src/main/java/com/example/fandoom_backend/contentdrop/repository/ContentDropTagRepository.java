package com.example.fandoom_backend.contentdrop.repository;

import com.example.fandoom_backend.contentdrop.entity.ContentDrop;
import com.example.fandoom_backend.contentdrop.entity.ContentDropTag;
import com.example.fandoom_backend.contentdrop.entity.SubjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// [content-drops-veri] merdiveninin dört basamağı: tam bölüm -> aynı sezon ->
// aynı yapım -> aynı evren. Her sorgu yalnız PUBLISHED content drop'ları döner.
public interface ContentDropTagRepository extends JpaRepository<ContentDropTag, Long> {

    @Query("SELECT DISTINCT t.contentDrop FROM ContentDropTag t "
            + "WHERE t.subjectType = :subjectType AND t.subjectId = :subjectId "
            + "AND t.seasonNumber = :seasonNumber AND t.episodeNumber = :episodeNumber "
            + "AND t.contentDrop.status = com.example.fandoom_backend.contentdrop.entity.ContentDropStatus.PUBLISHED "
            + "ORDER BY t.contentDrop.publishedAt DESC")
    List<ContentDrop> findByExactEpisode(
            @Param("subjectType") SubjectType subjectType,
            @Param("subjectId") Long subjectId,
            @Param("seasonNumber") Integer seasonNumber,
            @Param("episodeNumber") Integer episodeNumber);

    @Query("SELECT DISTINCT t.contentDrop FROM ContentDropTag t "
            + "WHERE t.subjectType = :subjectType AND t.subjectId = :subjectId "
            + "AND t.seasonNumber = :seasonNumber AND t.episodeNumber IS NULL "
            + "AND t.contentDrop.status = com.example.fandoom_backend.contentdrop.entity.ContentDropStatus.PUBLISHED "
            + "ORDER BY t.contentDrop.publishedAt DESC")
    List<ContentDrop> findBySameSeason(
            @Param("subjectType") SubjectType subjectType,
            @Param("subjectId") Long subjectId,
            @Param("seasonNumber") Integer seasonNumber);

    @Query("SELECT DISTINCT t.contentDrop FROM ContentDropTag t "
            + "WHERE t.subjectType = :subjectType AND t.subjectId = :subjectId "
            + "AND t.seasonNumber IS NULL AND t.episodeNumber IS NULL "
            + "AND t.contentDrop.status = com.example.fandoom_backend.contentdrop.entity.ContentDropStatus.PUBLISHED "
            + "ORDER BY t.contentDrop.publishedAt DESC")
    List<ContentDrop> findBySameProduction(
            @Param("subjectType") SubjectType subjectType,
            @Param("subjectId") Long subjectId);

    @Query("SELECT DISTINCT t.contentDrop FROM ContentDropTag t "
            + "WHERE t.franchiseId = :franchiseId "
            + "AND t.contentDrop.status = com.example.fandoom_backend.contentdrop.entity.ContentDropStatus.PUBLISHED "
            + "ORDER BY t.contentDrop.publishedAt DESC")
    List<ContentDrop> findBySameFranchise(@Param("franchiseId") Long franchiseId);
}
