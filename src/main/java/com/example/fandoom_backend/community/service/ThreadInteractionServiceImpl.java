package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.dto.ThreadBookmarkStatusResponse;
import com.example.fandoom_backend.community.dto.ThreadLikeStatusResponse;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadBookmark;
import com.example.fandoom_backend.community.entity.ThreadLike;
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
// Thread'e erişim id (birincil) veya slug (@Deprecated) ile; ikisi de yalnız PUBLISHED ve portalı HIDDEN olmayan
// thread'i bulur. Slug varyantları kendi @Transactional/@CacheEvict'ini taşır (self-invocation proxy'yi atlardı).
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
    public ThreadLikeStatusResponse like(Long userId, Long threadId) {
        return doLike(userId, findVisibleThread(threadId));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
    public ThreadLikeStatusResponse unlike(Long userId, Long threadId) {
        return doUnlike(userId, findVisibleThread(threadId));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
    public ThreadBookmarkStatusResponse bookmark(Long userId, Long threadId) {
        return doBookmark(userId, findVisibleThread(threadId));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
    public ThreadBookmarkStatusResponse unbookmark(Long userId, Long threadId) {
        return doUnbookmark(userId, findVisibleThread(threadId));
    }

    @Override
    @Deprecated
    @Transactional
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
    public ThreadLikeStatusResponse like(Long userId, String slug) {
        return doLike(userId, findVisibleThread(slug));
    }

    @Override
    @Deprecated
    @Transactional
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
    public ThreadLikeStatusResponse unlike(Long userId, String slug) {
        return doUnlike(userId, findVisibleThread(slug));
    }

    @Override
    @Deprecated
    @Transactional
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
    public ThreadBookmarkStatusResponse bookmark(Long userId, String slug) {
        return doBookmark(userId, findVisibleThread(slug));
    }

    @Override
    @Deprecated
    @Transactional
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
    public ThreadBookmarkStatusResponse unbookmark(Long userId, String slug) {
        return doUnbookmark(userId, findVisibleThread(slug));
    }

    // ARCHIVED portal yazma kapalı: like/bookmark eklenemez (unlike/unbookmark serbest — kullanıcı kendi durumunu geri alabilir).
    private void assertPortalWritable(Thread thread) {
        if (threadRepository.isInArchivedPortal(thread.getId())) {
            throw new InvalidReferenceException("Portal arşivlenmiş, bu işlem yapılamaz");
        }
    }

    private ThreadLikeStatusResponse doLike(Long userId, Thread thread) {
        assertPortalWritable(thread);
        boolean alreadyLiked = threadLikeRepository.existsByUserIdAndThreadId(userId, thread.getId());
        if (!alreadyLiked) {
            threadLikeRepository.save(ThreadLike.builder().userId(userId).threadId(thread.getId()).build());
            threadRepository.incrementLikeCount(thread.getId());
        }
        int count = alreadyLiked ? thread.getLikeCount() : thread.getLikeCount() + 1;
        return new ThreadLikeStatusResponse(true, count);
    }

    private ThreadLikeStatusResponse doUnlike(Long userId, Thread thread) {
        boolean wasLiked = threadLikeRepository.existsByUserIdAndThreadId(userId, thread.getId());
        if (wasLiked) {
            threadLikeRepository.deleteByUserIdAndThreadId(userId, thread.getId());
            threadRepository.decrementLikeCount(thread.getId());
        }
        int count = wasLiked ? thread.getLikeCount() - 1 : thread.getLikeCount();
        return new ThreadLikeStatusResponse(false, count);
    }

    private ThreadBookmarkStatusResponse doBookmark(Long userId, Thread thread) {
        assertPortalWritable(thread);
        boolean alreadyBookmarked = threadBookmarkRepository.existsByUserIdAndThreadId(userId, thread.getId());
        if (!alreadyBookmarked) {
            threadBookmarkRepository.save(ThreadBookmark.builder().userId(userId).threadId(thread.getId()).build());
            threadRepository.incrementBookmarkCount(thread.getId());
        }
        int count = alreadyBookmarked ? thread.getBookmarkCount() : thread.getBookmarkCount() + 1;
        return new ThreadBookmarkStatusResponse(true, count);
    }

    private ThreadBookmarkStatusResponse doUnbookmark(Long userId, Thread thread) {
        boolean wasBookmarked = threadBookmarkRepository.existsByUserIdAndThreadId(userId, thread.getId());
        if (wasBookmarked) {
            threadBookmarkRepository.deleteByUserIdAndThreadId(userId, thread.getId());
            threadRepository.decrementBookmarkCount(thread.getId());
        }
        int count = wasBookmarked ? thread.getBookmarkCount() - 1 : thread.getBookmarkCount();
        return new ThreadBookmarkStatusResponse(false, count);
    }

    private Thread findVisibleThread(Long id) {
        return threadRepository.findVisibleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thread bulunamadı: id=" + id));
    }

    private Thread findVisibleThread(String slug) {
        return threadRepository.findVisibleBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Thread bulunamadı: slug=" + slug));
    }
}
