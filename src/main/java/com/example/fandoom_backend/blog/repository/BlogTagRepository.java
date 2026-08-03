package com.example.fandoom_backend.blog.repository;

import com.example.fandoom_backend.blog.entity.Blog;
import com.example.fandoom_backend.blog.entity.BlogTag;
import com.example.fandoom_backend.blog.entity.SubjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// [blog-veri] merdiveninin dört basamağı: tam bölüm -> aynı sezon ->
// aynı yapım -> aynı evren. Her sorgu yalnız PUBLISHED blog'ları döner.
public interface BlogTagRepository extends JpaRepository<BlogTag, Long> {

    @Query("SELECT DISTINCT t.blog FROM BlogTag t "
            + "WHERE t.subjectType = :subjectType AND t.subjectId = :subjectId "
            + "AND t.seasonNumber = :seasonNumber AND t.episodeNumber = :episodeNumber "
            + "AND t.blog.status = com.example.fandoom_backend.blog.entity.BlogStatus.PUBLISHED "
            + "ORDER BY t.blog.publishedAt DESC")
    List<Blog> findByExactEpisode(
            @Param("subjectType") SubjectType subjectType,
            @Param("subjectId") Long subjectId,
            @Param("seasonNumber") Integer seasonNumber,
            @Param("episodeNumber") Integer episodeNumber);

    @Query("SELECT DISTINCT t.blog FROM BlogTag t "
            + "WHERE t.subjectType = :subjectType AND t.subjectId = :subjectId "
            + "AND t.seasonNumber = :seasonNumber AND t.episodeNumber IS NULL "
            + "AND t.blog.status = com.example.fandoom_backend.blog.entity.BlogStatus.PUBLISHED "
            + "ORDER BY t.blog.publishedAt DESC")
    List<Blog> findBySameSeason(
            @Param("subjectType") SubjectType subjectType,
            @Param("subjectId") Long subjectId,
            @Param("seasonNumber") Integer seasonNumber);

    @Query("SELECT DISTINCT t.blog FROM BlogTag t "
            + "WHERE t.subjectType = :subjectType AND t.subjectId = :subjectId "
            + "AND t.seasonNumber IS NULL AND t.episodeNumber IS NULL "
            + "AND t.blog.status = com.example.fandoom_backend.blog.entity.BlogStatus.PUBLISHED "
            + "ORDER BY t.blog.publishedAt DESC")
    List<Blog> findBySameProduction(
            @Param("subjectType") SubjectType subjectType,
            @Param("subjectId") Long subjectId);

    @Query("SELECT DISTINCT t.blog FROM BlogTag t "
            + "WHERE t.franchiseId = :franchiseId "
            + "AND t.blog.status = com.example.fandoom_backend.blog.entity.BlogStatus.PUBLISHED "
            + "ORDER BY t.blog.publishedAt DESC")
    List<Blog> findBySameFranchise(@Param("franchiseId") Long franchiseId);

    // Blog hub facet paneli: franchise başına DISTINCT blog sayısı (bir
    // blog'un aynı franchise'a birden fazla BlogTag satırıyla bağlı olması
    // durumunda mükerrer saymamak için DISTINCT şart). NOT: draft/published
    // ayrımı yapılmaz (bkz. BlogQueryServiceImpl.getFacets Javadoc'u) — bu
    // sorgu bilinçli olarak status filtresi içermez, tag/ modülünün blog
    // durumu hakkında bilgi sahibi olmasını gerektirmemek için.
    @Query("SELECT bt.franchiseId, COUNT(DISTINCT bt.blog.id) FROM BlogTag bt "
            + "WHERE bt.franchiseId IS NOT NULL GROUP BY bt.franchiseId")
    List<Object[]> countDistinctBlogsByFranchiseId();
}
