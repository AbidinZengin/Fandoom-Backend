package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.KeysetPageResponse;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.ThreadDetailResponse;
import com.example.fandoom_backend.community.dto.ThreadPatchRequest;
import com.example.fandoom_backend.community.dto.ThreadRequest;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ThreadService {

    // viewerId: giriş yapmamış istekte null — isLiked/isBookmarked hep false
    // döner, ekstra sorgu atılmaz. tags: null/boş ise filtre uygulanmaz, birden
    // fazla tag VEYA (OR) mantığıyla birleşir (herhangi birine sahip thread'ler).
    // portalSlug: null ise portal filtresi yok (HIDDEN portalların thread'leri yine de dışlanır); yok/HIDDEN portal -> 404.
    PageResponse<ThreadSummaryResponse> list(
            ThreadSurface surface, String portalSlug, String productionSlug, List<String> tags, String sort,
            Long viewerId, Pageable pageable);

    // Keyset (cursor) varyantı — derin sayfada OFFSET/COUNT maliyeti yok. cursor: önceki
    // yanıttaki nextCursor (ilk sayfada null).
    KeysetPageResponse<ThreadSummaryResponse> listByCursor(
            ThreadSurface surface, String portalSlug, String productionSlug, List<String> tags, String sort,
            Long viewerId, String cursor, int size);

    // scope=joined: yalnız viewerId'nin üye olduğu (HIDDEN olmayan) portalların thread'leri; portalSlug verilirse ve
    // kullanıcı o portala üye değilse sonuç boş. ASLA cache'lenmez (kullanıcıya özel küme).
    PageResponse<ThreadSummaryResponse> listJoined(
            Long viewerId, ThreadSurface surface, String portalSlug, String productionSlug, List<String> tags,
            String sort, Pageable pageable);

    KeysetPageResponse<ThreadSummaryResponse> listJoinedByCursor(
            Long viewerId, ThreadSurface surface, String portalSlug, String productionSlug, List<String> tags,
            String sort, String cursor, int size);

    // Birincil okuma yolu: slug title'dan üretilir ve başlık PATCH'lenince değişir; id kalıcıdır.
    // Yok / DELETED / HIDDEN thread ve portalı HIDDEN olan thread -> 404.
    ThreadDetailResponse getById(Long id, Long viewerId);

    /**
     * @deprecated slug title değişince değişir (link kırılır); {@link #getById(Long, Long)} kullanın.
     */
    @Deprecated
    ThreadDetailResponse getBySlug(String slug, Long viewerId);

    // moderator = MODERATOR/ADMIN: STAFF_ONLY portallara yazabilme yetkisi.
    ThreadDetailResponse create(Long authorId, boolean moderator, ThreadRequest request);

    // Id varyantları birincil yol (sayısal path segmenti). Slug varyantları @Deprecated.
    ThreadDetailResponse update(Long userId, boolean moderator, Long threadId, ThreadPatchRequest request);

    void delete(Long userId, boolean moderator, Long threadId);

    /** @deprecated slug başlıkla değişir; {@link #update(Long, boolean, Long, ThreadPatchRequest)} kullanın. */
    @Deprecated
    ThreadDetailResponse update(Long userId, boolean moderator, String slug, ThreadPatchRequest request);

    /** @deprecated slug başlıkla değişir; {@link #delete(Long, boolean, Long)} kullanın. */
    @Deprecated
    void delete(Long userId, boolean moderator, String slug);
}
