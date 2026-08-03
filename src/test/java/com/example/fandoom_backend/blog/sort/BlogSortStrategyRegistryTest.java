package com.example.fandoom_backend.blog.sort;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BlogSortStrategyRegistryTest {

    private final BlogSortStrategyRegistry registry = new BlogSortStrategyRegistry(List.of(
            new LatestBlogSortStrategy(), new OldestBlogSortStrategy(),
            new TrendingBlogSortStrategy(), new RecommendedBlogSortStrategy()));

    @Test
    void resolve_latest_returnsPublishedAtDescending() {
        assertThat(registry.resolve("latest")).isEqualTo(Sort.by(Sort.Direction.DESC, "publishedAt"));
    }

    @Test
    void resolve_oldest_returnsPublishedAtAscending() {
        assertThat(registry.resolve("oldest")).isEqualTo(Sort.by(Sort.Direction.ASC, "publishedAt"));
    }

    @Test
    void resolve_trending_returnsViewCountDescending() {
        assertThat(registry.resolve("trending")).isEqualTo(Sort.by(Sort.Direction.DESC, "viewCount"));
    }

    @Test
    void resolve_recommended_returnsRecommendedRankAscendingWithNullsLast() {
        Sort.Order order = registry.resolve("recommended").getOrderFor("recommendedRank");

        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(order.getNullHandling()).isEqualTo(Sort.NullHandling.NULLS_LAST);
    }

    @Test
    void resolve_unknownKey_throwsInvalidReferenceException() {
        assertThatThrownBy(() -> registry.resolve("bogus")).isInstanceOf(InvalidReferenceException.class);
    }
}
