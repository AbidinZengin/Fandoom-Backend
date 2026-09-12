# Community Modülü — Devam Planı

**Tarih:** 2026-09-12
**Durum:** Faz 1 implementasyonu tamamlandı (commit `a101db9`, `community/` paketi). Faz 2-4 henüz kod yazılmadı — bu doküman devam planı.

## Amaç

Fandoom Community bölümü: kullanıcı üretimi thread'ler (Discussion/Theory/Fan Art), yorumlar, like/bookmark, tag sistemi, herkese açık profil ve zamanla kişiselleştirilmiş feed. Aşağıdaki fazlandırma orijinal tasarım dokümanına dayanır; her madde gerçek koda göre ✅/⚠️/❌ ile işaretlendi.

## Mevcut Backend Konvansiyonlarına Uyum

```
Auth:     JWT Bearer token — mevcut SecurityConfig kuralları
Pageable: PageResponse<T> { content, page, size, totalElements, totalPages, last }
Error:    ApiErrorResponse { message, fieldErrors? } — GlobalExceptionHandler
Lang:     Accept-Language header — mevcut i18n altyapısı
Slug:     backend üretir (SlugGenerator.generateUnique, çakışma korumalı)
```

---

## Faz 1 — Tamamlandı ✅ (`community/` paketi)

| Kalem | Durum |
|---|---|
| Thread CRUD + slug üretimi | ✅ `ThreadServiceImpl` |
| ThreadTag (serbest, `tag/` modülünden bağımsız) | ✅ |
| Comment (2 seviye sabit) | ✅ `CommentServiceImpl.create` — `parent.getParent() != null` kontrolü |
| Like/Bookmark toggle (thread + comment), idempotent | ✅ `ThreadInteractionServiceImpl`, `CommentInteractionServiceImpl` |
| `GET /api/community/feed` | ✅ ama `CommunityFeedServiceImpl` şu an `ThreadService.list`'e ince bir delege — kişiselleştirme yok (bilerek, Faz 4 bekliyor) |
| `GET /api/community/threads/:slug` | ✅ |
| Soft delete (`status=DELETED`, body `"[silindi]"` maskesi) | ✅ — proje genelindeki "hard delete" kuralına bilinçli istisna |
| Validasyon limitleri (title 10-200, body ≤10000, comment 2-2000, tag ≤10/thread, tag ≤50 char) | ✅ birebir |
| Yazma yetkilendirmesi: herhangi giriş yapmış kullanıcı, sahip/moderatör kontrolü serviste | ✅ `SecurityConfig` + `assertOwnerOrModerator` |

### Faz 1 içinde açık kalan eksikler (yeni faz değil, mevcut fazın tamamlanmamış parçaları)

- **Yazar zenginleştirmesi yok**: `ThreadSummaryResponse`/`ThreadDetailResponse`/`CommentResponse` sadece ham `authorId` (Long) döner; planın istediği `author: { username, avatarUrl }` nested objesi yok. `production/`'daki gibi bir orkestrasyon katmanı (`UserService` inject) gerekecek.
- **`isLiked`/`isBookmarked` per-viewer alanları yok**: JWT'li istekte bile liste/detay response'u kullanıcının kendi beğeni/bookmark durumunu taşımıyor — client ayrıca sormak zorunda.
- **`excerpt` alanı yok**: `ThreadSummaryResponse`'da body'den türetilmiş 160 karakterlik kısaltma yok.
- **Rate limiting yok**: plan thread için 10/saat, comment için 30/saat öngörüyor; şu an sadece login'de (`AuthRateLimitFilter`) rate limit var, community yazma uçlarında hiç yok.
- **`hotScore`/`qualityScore` hiç hesaplanmıyor**: kolonlar var, `HotScoreJob` (15 dakikada bir) hiç yazılmadı — `sort=hot` şu an fiilen sabit `0`'a göre sıralıyor.
- **`commentCount` sayacı asimetrik**: `incrementCommentCount` var ama `decrementCommentCount` yok; bir yorum silinse bile thread'in `commentCount`'u düşmüyor (like/bookmark'ta increment+decrement ikisi de var). Kasıtlı mı yoksa eksik mi netleştirilmeli.

---

## Faz 2 — Community Profile ❌ (başlanmadı)

Herkese açık kullanıcı profili. `account/` modülündeki (şu an uncommitted WIP) profil/liste/follow mekanizmasıyla örtüşme ihtimali var — implementasyona başlamadan önce kontrol edilmeli.

```
GET /api/users/:username/profile   → CommunityProfileResponse
GET /api/users/:username/threads   → PageResponse<ThreadSummaryResponse>
GET /api/users/:username/likes     → PageResponse<ThreadSummaryResponse> (kullanıcı gizliyse 403)
GET /api/users/:username/bookmarks → PageResponse<ThreadSummaryResponse> (Auth: sadece kendi hesabı)
```

```json
// CommunityProfileResponse
{
  "username": "valyrian_scrolls", "avatarUrl": "...", "bannerUrl": "...",
  "bio": "...", "fandomTitle": "Büyücü", "memberSince": "2024-03-15",
  "stats": { "threadCount": 47, "theoryCount": 23, "likeReceived": 1830,
             "commentCount": 312, "followerCount": 89, "followingCount": 34 },
  "isFollowing": false, "isOwnProfile": false
}
```

**Not:** `stats` hesaplaması muhtemelen `community/` + `account/`'ın follow verisini birleştiren bir orkestrasyon servisi gerektirir (`production/` modülündeki desene benzer).

---

## Faz 3 — Tag Sistemi ⚠️ (kısmen var)

| Kalem | Durum |
|---|---|
| `GET /api/community/threads?tag=X` (tag'e göre filtre) | ✅ zaten var, `ThreadSpecificationBuilder.hasIdIn` üzerinden |
| `GET /api/community/tags/trending` | ❌ yok — `TrendingTagsJob` (saatlik, tag frekansı cache) hiç yazılmadı |
| `GET /api/community/tags/:tag/threads` (ayrı path, threadCount ile) | ❌ yok — şu anki `?tag=` filtresi aynı işi görüyor ama dedicated response şekli (threadCount) yok |
| `TagFollow` entity + `POST/DELETE /api/me/follows/TAG/:tag` | ❌ hiç yok |

---

## Faz 4 — Kişiselleştirme ❌ (başlanmadı)

```
UserEvent            — ham sinyal akışı (VIEW_CARD, CLICK_THREAD, DWELL_THREAD, ...), 90 gün retention
UserInterestProfile  — userId başına productions/tags/surfaces/authors ağırlık vektörü (JSONB)
FeedRankingService   — computeHotScore (Wilson score + velocity + decay) + computeRelevance + diversityFactor
```

```
POST   /api/me/events/batch              — 202 Accepted, async kuyruk
POST   /api/community/threads/:slug/not-interested
GET    /api/me/interest-profile          — GDPR şeffaflık
DELETE /api/me/interest-profile          — cold start'a dön
GET    /api/community/feed?sort=personalized
```

Scheduled job'lar: `HotScoreJob` (15 dk), `InterestProfileJob` (gece 02:00, decay dahil), `EventPurgeJob` (haftalık, 90 gün), `TrendingTagsJob` (saatlik).

**Not:** `HotScoreJob` aslında Faz 1'in bir parçası sayılabilir (Faz 1'in `sort=hot`'unu anlamlı kılmak için) — Faz 4'ü beklemeden, bağımsız erken yapılabilir (bkz. Faz 1 açık eksikleri).

---

## Veritabanı — Faz 2-4 için gerekecek indeksler

```sql
-- Event sorguları (Faz 4, purge için)
CREATE INDEX idx_user_event_timestamp ON user_event(timestamp);
CREATE INDEX idx_user_event_user      ON user_event(user_id);

-- Tag follow (Faz 3)
-- PK (userId, tag) zaten yeterli index sağlar
```

(Thread/Comment/Tag indeksleri Faz 1'de zaten kuruldu — bkz. `Thread`/`Comment`/`ThreadTag` entity `@Index` tanımları.)

---

## Önerilen Sıra

1. **Faz 1 açık eksiklerini kapat** (yazar enrichment, `isLiked`/`isBookmarked`, `HotScoreJob`) — bunlar olmadan feed/detay sayfaları frontend'de eksik/yanlış görünür.
2. **Faz 2 (Community Profile)** — `account/` modülünün mevcut WIP'iyle çakışmayı önce netleştir.
3. **Faz 3 (Tag sistemi)** — trending + follow, düşük efor.
4. **Faz 4 (Kişiselleştirme)** — en yüksek efor, en son.
