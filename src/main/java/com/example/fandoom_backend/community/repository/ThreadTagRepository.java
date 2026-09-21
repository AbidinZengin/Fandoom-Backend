package com.example.fandoom_backend.community.repository;

import com.example.fandoom_backend.community.dto.TrendingTagResponse;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.entity.ThreadTag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ThreadTagRepository extends JpaRepository<ThreadTag, Long> {

    List<ThreadTag> findByThread_Id(Long threadId);

    List<ThreadTag> findByThread_IdIn(Collection<Long> threadIds);

    // GET /threads?tags=x,y filtresi için — thread.id'ye map edilir
    // (ThreadServiceImpl). Birden fazla tag VEYA (OR) mantığıyla eşleşir:
    // herhangi birine sahip thread'ler döner.
    List<ThreadTag> findByTagIn(Collection<String> tags);

    void deleteByThread_Id(Long threadId);

    // TagFollowServiceImpl.listFollowed için: bir tag'i taşıyan PUBLISHED
    // thread sayısı (silinmiş/gizli thread'ler sayılmaz).
    // HIDDEN portaldaki thread'ler sayılmaz (portal ve içeriği public tarafta "yok" sayılır — tag sayısı da sızdırmamalı).
    @Query("SELECT COUNT(tt) FROM ThreadTag tt WHERE tt.tag = :tag AND tt.thread.status = :status AND NOT EXISTS (SELECT 1 FROM Portal p WHERE p.id = tt.thread.portalId AND p.status = com.example.fandoom_backend.community.entity.PortalStatus.HIDDEN)")
    long countByTagAndThread_Status(@Param("tag") String tag, @Param("status") ThreadStatus status);

    // Trending tags: job/cache YOK, on-the-fly agregasyon (production/
    // modülündeki "bilinçli basit çözüm" trade-off'una benzer — bu ölçekte
    // yeterli). JPQL constructor expression, TagRepository.findFacetOptions
    // ile aynı desen. Pageable, LIMIT'i (page 0, size=limit) ifade etmek için
    // kullanılır, gerçek sayfalama yok.
    @Query("SELECT new com.example.fandoom_backend.community.dto.TrendingTagResponse(tt.tag, COUNT(tt)) "
            + "FROM ThreadTag tt "
            + "WHERE tt.thread.status = :status AND tt.thread.createdAt >= :since "
            + "AND NOT EXISTS (SELECT 1 FROM Portal p WHERE p.id = tt.thread.portalId AND p.status = com.example.fandoom_backend.community.entity.PortalStatus.HIDDEN) "
            + "GROUP BY tt.tag ORDER BY COUNT(tt) DESC")
    List<TrendingTagResponse> findTrending(
            @Param("status") ThreadStatus status, @Param("since") LocalDateTime since, Pageable pageable);
}
