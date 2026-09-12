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

    // GET /threads?tag=x filtresi için — thread.id'ye map edilir (ThreadServiceImpl).
    List<ThreadTag> findByTag(String tag);

    void deleteByThread_Id(Long threadId);

    // TagFollowServiceImpl.listFollowed için: bir tag'i taşıyan PUBLISHED
    // thread sayısı (silinmiş/gizli thread'ler sayılmaz).
    long countByTagAndThread_Status(String tag, ThreadStatus status);

    // Trending tags: job/cache YOK, on-the-fly agregasyon (production/
    // modülündeki "bilinçli basit çözüm" trade-off'una benzer — bu ölçekte
    // yeterli). JPQL constructor expression, TagRepository.findFacetOptions
    // ile aynı desen. Pageable, LIMIT'i (page 0, size=limit) ifade etmek için
    // kullanılır, gerçek sayfalama yok.
    @Query("SELECT new com.example.fandoom_backend.community.dto.TrendingTagResponse(tt.tag, COUNT(tt)) "
            + "FROM ThreadTag tt "
            + "WHERE tt.thread.status = :status AND tt.thread.createdAt >= :since "
            + "GROUP BY tt.tag ORDER BY COUNT(tt) DESC")
    List<TrendingTagResponse> findTrending(
            @Param("status") ThreadStatus status, @Param("since") LocalDateTime since, Pageable pageable);
}
