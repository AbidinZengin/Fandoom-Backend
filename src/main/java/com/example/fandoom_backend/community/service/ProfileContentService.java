package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.account.dto.UserListSummaryResponse;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.dto.UserCommentResponse;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import org.springframework.data.domain.Pageable;

import java.util.List;

// Public profil sekmelerinin (thread'ler, yorumlar, listeler) ortak girişi. Hepsi username ile çözülür
// (bilinmeyen → 404) ve gizli profil denetimini TEK yerde uygular: izleyici kısıtlıysa (profil gizli, izleyici
// sahibi değil ve onaylı takipçi değil) 403 değil boş sayfa/liste döner, FE kilidi restricted bayrağından çizer.
// viewerId nullable (anonim → gizli profilde her zaman kısıtlı).
public interface ProfileContentService {

    // author filtresi için: thread'ler. username boş/whitespace → 400. Cache'lenmez.
    PageResponse<ThreadSummaryResponse> threads(
            String username, ThreadSurface surface, String sort, Long viewerId, Pageable pageable);

    PageResponse<UserCommentResponse> comments(String username, Long viewerId, Pageable pageable);

    // Herkese açık CUSTOM listeler, pinliler önce.
    List<UserListSummaryResponse> lists(String username, Long viewerId);

    // Mevcut pinli-liste ucu; artık gizli profil denetiminden geçer.
    List<UserListSummaryResponse> pinnedLists(String username, Long viewerId);
}
