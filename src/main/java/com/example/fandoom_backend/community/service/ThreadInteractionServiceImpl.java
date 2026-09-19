package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.dto.ThreadBookmarkStatusResponse;
import com.example.fandoom_backend.community.dto.ThreadLikeStatusResponse;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadBookmark;
import com.example.fandoom_backend.community.entity.ThreadLike;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.repository.ThreadBookmarkRepository;
import com.example.fandoom_backend.community.repository.ThreadLikeRepository;
import com.example.fandoom_backend.community.repository.ThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Like/bookmark toggle idempotent: zaten like'lanmış bir thread'e tekrar
// POST atmak hata döndürmez, mevcut durumu aynen döner (çift tıklama UX'i).
// Sayaçlar ThreadRepository'deki native @Modifying sorgularla güncellenir;
// entity'nin in-memory alanı BİLEREK set edilmez (dirty-checking'in bulk
// update'i stale değerle ezmesini önlemek için) — dönüş değeri sadece
// getLikeCount()+1/-1 ile hesaplanır.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThreadInteractionServiceImpl implements ThreadInteractionService {

    private final ThreadRepository threadRepository;
    private final ThreadLikeRepository threadLikeRepository;
    private final ThreadBookmarkRepository threadBookmarkRepository;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
    public ThreadLikeStatusResponse like(Long userId, String slug) {
        Thread thread = findPublishedThread(slug);
        boolean alreadyLiked = threadLikeRepository.existsByUserIdAndThreadId(userId, thread.getId());
        if (!alreadyLiked) {
            threadLikeRepository.save(ThreadLike.builder().userId(userId).threadId(thread.getId()).build());
            threadRepository.incrementLikeCount(thread.getId());
        }
        int count = alreadyLiked ? thread.getLikeCount() : thread.getLikeCount() + 1;
        return new ThreadLikeStatusResponse(true, count);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
    public ThreadLikeStatusResponse unlike(Long userId, String slug) {
        Thread thread = findPublishedThread(slug);
        boolean wasLiked = threadLikeRepository.existsByUserIdAndThreadId(userId, thread.getId());
        if (wasLiked) {
            threadLikeRepository.deleteByUserIdAndThreadId(userId, thread.getId());
            threadRepository.decrementLikeCount(thread.getId());
        }
        int count = wasLiked ? thread.getLikeCount() - 1 : thread.getLikeCount();
        return new ThreadLikeStatusResponse(false, count);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
    public ThreadBookmarkStatusResponse bookmark(Long userId, String slug) {
        Thread thread = findPublishedThread(slug);
        boolean alreadyBookmarked = threadBookmarkRepository.existsByUserIdAndThreadId(userId, thread.getId());
        if (!alreadyBookmarked) {
            threadBookmarkRepository.save(ThreadBookmark.builder().userId(userId).threadId(thread.getId()).build());
            threadRepository.incrementBookmarkCount(thread.getId());
        }
        int count = alreadyBookmarked ? thread.getBookmarkCount() : thread.getBookmarkCount() + 1;
        return new ThreadBookmarkStatusResponse(true, count);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
    public ThreadBookmarkStatusResponse unbookmark(Long userId, String slug) {
        Thread thread = findPublishedThread(slug);
        boolean wasBookmarked = threadBookmarkRepository.existsByUserIdAndThreadId(userId, thread.getId());
        if (wasBookmarked) {
            threadBookmarkRepository.deleteByUserIdAndThreadId(userId, thread.getId());
            threadRepository.decrementBookmarkCount(thread.getId());
        }
        int count = wasBookmarked ? thread.getBookmarkCount() - 1 : thread.getBookmarkCount();
        return new ThreadBookmarkStatusResponse(false, count);
    }

    private Thread findPublishedThread(String slug) {
        return threadRepository.findBySlugAndStatus(slug, ThreadStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Thread bulunamadı: slug=" + slug));
    }
}
