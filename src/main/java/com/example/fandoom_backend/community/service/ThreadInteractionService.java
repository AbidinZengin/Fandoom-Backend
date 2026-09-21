package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.community.dto.ThreadBookmarkStatusResponse;
import com.example.fandoom_backend.community.dto.ThreadLikeStatusResponse;

// Id varyantları birincil yoldur (slug başlık PATCH'lenince değişir); slug varyantları geriye uyumluluk için @Deprecated.
// Yok / DELETED / HIDDEN thread ve portalı HIDDEN olan thread -> 404.
public interface ThreadInteractionService {

    ThreadLikeStatusResponse like(Long userId, Long threadId);

    ThreadLikeStatusResponse unlike(Long userId, Long threadId);

    ThreadBookmarkStatusResponse bookmark(Long userId, Long threadId);

    ThreadBookmarkStatusResponse unbookmark(Long userId, Long threadId);

    /** @deprecated slug değişebilir; {@link #like(Long, Long)} kullanın. */
    @Deprecated
    ThreadLikeStatusResponse like(Long userId, String slug);

    /** @deprecated slug değişebilir; {@link #unlike(Long, Long)} kullanın. */
    @Deprecated
    ThreadLikeStatusResponse unlike(Long userId, String slug);

    /** @deprecated slug değişebilir; {@link #bookmark(Long, Long)} kullanın. */
    @Deprecated
    ThreadBookmarkStatusResponse bookmark(Long userId, String slug);

    /** @deprecated slug değişebilir; {@link #unbookmark(Long, Long)} kullanın. */
    @Deprecated
    ThreadBookmarkStatusResponse unbookmark(Long userId, String slug);
}
