package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.util.SlugGenerator;
import com.example.fandoom_backend.community.dto.FollowedTagResponse;
import com.example.fandoom_backend.community.dto.TagFollowStatusResponse;
import com.example.fandoom_backend.community.dto.TrendingTagResponse;
import com.example.fandoom_backend.community.entity.TagFollow;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.repository.TagFollowRepository;
import com.example.fandoom_backend.community.repository.ThreadTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// ThreadLike/ThreadBookmark ile aynı idempotent toggle deseni. Trending için
// ayrı bir job/cache yok (bkz. ThreadTagRepository.findTrending) — bu ölçekte
// basit on-the-fly agregasyon yeterli.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagFollowServiceImpl implements TagFollowService {

    private final TagFollowRepository tagFollowRepository;
    private final ThreadTagRepository threadTagRepository;

    @Override
    @Transactional
    public TagFollowStatusResponse follow(Long userId, String tag) {
        String normalized = SlugGenerator.slugify(tag);
        if (!tagFollowRepository.existsByUserIdAndTag(userId, normalized)) {
            tagFollowRepository.save(TagFollow.builder().userId(userId).tag(normalized).build());
        }
        return new TagFollowStatusResponse(true);
    }

    @Override
    @Transactional
    public TagFollowStatusResponse unfollow(Long userId, String tag) {
        String normalized = SlugGenerator.slugify(tag);
        tagFollowRepository.deleteByUserIdAndTag(userId, normalized);
        return new TagFollowStatusResponse(false);
    }

    @Override
    public List<FollowedTagResponse> listFollowed(Long userId) {
        return tagFollowRepository.findByUserId(userId).stream()
                .map(tf -> new FollowedTagResponse(
                        tf.getTag(),
                        threadTagRepository.countByTagAndThread_Status(tf.getTag(), ThreadStatus.PUBLISHED)))
                .toList();
    }

    @Override
    public List<TrendingTagResponse> getTrending(String window, int limit) {
        LocalDateTime since = resolveSince(window);
        return threadTagRepository.findTrending(ThreadStatus.PUBLISHED, since, Pageable.ofSize(limit));
    }

    private LocalDateTime resolveSince(String window) {
        return switch (window == null ? "7d" : window) {
            case "30d" -> LocalDateTime.now().minusDays(30);
            default -> LocalDateTime.now().minusDays(7);
        };
    }
}
