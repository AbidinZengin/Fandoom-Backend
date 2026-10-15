package com.example.fandoom_backend.blog.repository;

import com.example.fandoom_backend.blog.dto.BlogSearchResult;
import com.example.fandoom_backend.blog.entity.Blog;
import com.example.fandoom_backend.blog.entity.BlogStatus;
import com.example.fandoom_backend.common.util.LikePatterns;
import com.example.fandoom_backend.common.util.SearchFold;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BlogRepository extends JpaRepository<Blog, Long>, JpaSpecificationExecutor<Blog> {

    Optional<Blog> findBySlug(String slug);

    // Detay okuma yolu: blocks JOIN FETCH; tags/outgoingRelations bag olduğu için aynı grafta
    // olamaz, batch fetch ile gelir.
    @EntityGraph(attributePaths = "blocks")
    Optional<Blog> findWithBlocksById(Long id);

    @EntityGraph(attributePaths = "blocks")
    Optional<Blog> findWithBlocksBySlugAndStatus(String slug, BlogStatus status);

    Optional<Blog> findBySlugAndStatus(String slug, BlogStatus status);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Modifying
    @Query("UPDATE Blog b SET b.viewCount = b.viewCount + 1 WHERE b.id = :id")
    void incrementViewCount(@Param("id") Long id);

    // "Recency+popülerlik" fallback kademesi — merdivenin son basamağı.
    List<Blog> findByStatusOrderByPublishedAtDescViewCountDesc(BlogStatus status, Pageable pageable);

    List<Blog> findByStatusAndIdNotInOrderByPublishedAtDescViewCountDesc(
            BlogStatus status, Collection<Long> excludedIds, Pageable pageable);

    // Blog hub facet paneli: format artık Blog'un kendi alanı olduğu için
    // (tag/ modülünün aksine) status filtresi burada doğrudan uygulanabilir —
    // yalnızca yayınlanmış bloglar sayılır.
    @Query("SELECT b.format, COUNT(b) FROM Blog b "
            + "WHERE b.status = :status AND b.format IS NOT NULL GROUP BY b.format")
    List<Object[]> countPublishedByFormat(@Param("status") BlogStatus status);

    // Arama (quick + full, MovieRepository ile aynı kurallar ve "tek metin" deseni): yalnız verilen statü, title/titleTr'de
    // "içinde geçen"; önce "ile başlayan", sonra title, sonra id. Parantez şart: aksi halde taslaklar sızardı.
    String SEARCH_WHERE = " WHERE b.status = :status"
            + " AND (" + SearchFold.SQL_OPEN + "b.title" + SearchFold.SQL_CLOSE + " LIKE :contains" + LikePatterns.ESCAPE
            + " OR " + SearchFold.SQL_OPEN + "b.titleTr" + SearchFold.SQL_CLOSE + " LIKE :contains" + LikePatterns.ESCAPE + ")";
    String SEARCH_ORDER = " ORDER BY CASE WHEN " + SearchFold.SQL_OPEN + "b.title" + SearchFold.SQL_CLOSE + " LIKE :prefix" + LikePatterns.ESCAPE
            + " OR " + SearchFold.SQL_OPEN + "b.titleTr" + SearchFold.SQL_CLOSE + " LIKE :prefix" + LikePatterns.ESCAPE
            + " THEN 0 ELSE 1 END, b.title ASC, b.id ASC";

    @Query("SELECT new com.example.fandoom_backend.blog.dto.BlogSearchResult("
            + "b.id, b.slug, b.title, b.titleTr, b.kicker, b.kickerTr, b.imageUrl) FROM Blog b" + SEARCH_WHERE + SEARCH_ORDER)
    List<BlogSearchResult> quickSearch(@Param("status") BlogStatus status,
                                       @Param("contains") String containsPattern,
                                       @Param("prefix") String prefixPattern, Pageable pageable);

    @Query(value = "SELECT b FROM Blog b" + SEARCH_WHERE + SEARCH_ORDER,
            countQuery = "SELECT COUNT(b) FROM Blog b" + SEARCH_WHERE)
    Page<Blog> fullSearch(@Param("status") BlogStatus status,
                          @Param("contains") String containsPattern,
                          @Param("prefix") String prefixPattern, Pageable pageable);
}
