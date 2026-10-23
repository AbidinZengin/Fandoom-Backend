package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.FollowStateResponse;
import com.example.fandoom_backend.account.entity.FollowState;
import com.example.fandoom_backend.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

// Kişi takibi. Yaprak servis: yalnızca repository + UserService'e bağlıdır (profil verisi için
// community/'ye BAĞLANMAZ — döngü olmasın; profil zenginleştirmesi FollowListService'te).
public interface ProfileFollowService {
    // Idempotent. Kendini takip → 400, bilinmeyen username → 404.
    FollowStateResponse follow(Long followerId, String username);
    // Idempotent — takip yoksa no-op.
    FollowStateResponse unfollow(Long followerId, String username);
    FollowStateResponse getState(Long followerId, String username);

    FollowState stateOf(Long viewerId, Long userId);
    // viewerId'nin userIds ile ilişkileri, tek sorgu. İlişkisiz kullanıcı map'te yer almaz (= NONE).
    Map<Long, FollowState> statesOf(Long viewerId, Collection<Long> userIds);
    long countFollowers(Long userId);
    long countFollowing(Long userId);

    // En yeni takip önce; yalnız FOLLOWING durumundakiler.
    PageResponse<Long> listFollowerIds(Long userId, Pageable pageable);
    PageResponse<Long> listFollowingIds(Long userId, Pageable pageable);
    // ids içinden viewerId'nin takip ettikleri (liste satırlarındaki "following" bayrağı).
    Set<Long> filterFollowedBy(Long viewerId, Collection<Long> ids);

    // ---- gizli profil ----

    // Profil gizli, izleyici sahibi değil ve onaylı takipçi değil. Anonim izleyici de kısıtlıdır.
    boolean isRestricted(Long viewerId, Long userId);
    // userId'ye gelen bekleyen istekler (REQUESTED), en yeni önce.
    PageResponse<Long> listRequesterIds(Long userId, Pageable pageable);
    // Idempotent (zaten FOLLOWING ise no-op). Bekleyen istek yoksa 404.
    void acceptRequest(Long userId, String requesterUsername);
    // Yalnız REQUESTED satırını siler; onaylı takipçiyi çıkarmaz (bunun için removeFollower). İstek yoksa no-op.
    void rejectRequest(Long userId, String requesterUsername);
    // Mevcut takipçiyi çıkarır (FOLLOWING satırı). Takipçi değilse no-op.
    void removeFollower(Long userId, String followerUsername);
    // Gizlilik kapanınca: bekleyen tüm istekleri onaylar, onaylanan sayıyı döner.
    int approveAllRequests(Long userId);
}
