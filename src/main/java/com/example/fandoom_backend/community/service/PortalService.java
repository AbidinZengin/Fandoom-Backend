package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.community.dto.PortalRefResponse;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

// Thread/feed akışlarının Portal'dan ihtiyaç duyduğu dar arayüz (admin CRUD -> PortalAdminService).
// HIDDEN portal public tarafta "yok" sayılır: resolve* metotları 404 (ResourceNotFoundException) fırlatır.
public interface PortalService {

    // Okuma/feed filtresi için: portal yok veya HIDDEN ise 404. ARCHIVED okunabilir.
    Long resolveVisibleId(String slug);

    // Thread yazma için: yok/HIDDEN -> 404, ARCHIVED -> 400 (InvalidReferenceException),
    // STAFF_ONLY ve staff değilse -> 403 (AccessDeniedException). staff = MODERATOR/ADMIN.
    Long resolvePostingPortalId(String slug, boolean staff);

    // Thread'i başka portala TAŞIMA hedefi (moderatör/admin): yok/HIDDEN -> 404, ARCHIVED -> 400. STAFF_ONLY serbest
    // (taşıyan zaten staff).
    Long resolveMoveTargetId(String slug);

    // scope=joined: kullanıcının üye olduğu, HIDDEN olmayan (ARCHIVED dahil) portal id'leri.
    Set<Long> getJoinedVisiblePortalIds(Long userId);

    // productionSlug null ise no-op; portalın yapımlarından değilse 400.
    void assertProductionBelongsToPortal(Long portalId, String productionSlug);

    // Toplu çözüm (sayfa başına tek sorgu). HIDDEN portal için ref DÖNMEZ (map'te anahtar yok -> çağıran null görür):
    // HIDDEN portalın slug/adı hiçbir yanıtta sızmaz.
    Map<Long, PortalRefResponse> getRefsByIds(Collection<Long> portalIds);

    // Public thread listelerinden dışlanacak (HIDDEN) portal id'leri.
    Set<Long> getHiddenPortalIds();

    // Thread taşıma: eski portalın sayacı -1, yenisininki +1, TEK transaction'da. Güncelleme sırası portal id'sine göre
    // sabit (karşılıklı iki taşımanın satır kilitlerinde deadlock'a düşmemesi için). Yalnız PUBLISHED thread'ler için çağrılır.
    void moveThreadCount(Long fromPortalId, Long toPortalId);

    void incrementThreadCount(Long portalId);

    void decrementThreadCount(Long portalId);
}
