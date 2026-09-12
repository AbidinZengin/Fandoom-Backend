# Community Modülü — Devam Planı

**Tarih:** 2026-09-12 (son güncelleme: 2026-09-12)
**Durum:** Faz 1 tamamlandı (`a101db9` + açık eksikler `a678705`/`b9a96a7`'de kapandı). Faz 2 (Community Profile, daraltılmış kapsamla) tamamlandı (`22dd686`). Faz 3 (Tag Sistemi) tamamlandı. Faz 4 henüz kod yazılmadı.

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

### Faz 1 içinde açık kalan eksikler — HEPSİ KAPANDI ✅ (`a678705`, `b9a96a7`)

- ✅ **Yazar zenginleştirmesi** — `AuthorSummary(id, username)`, `UserService.getUsernamesByIds` ile toplu çözülüyor. **`avatarUrl` hâlâ yok** (bilinçli: `account/`'a bağımlılık eklenmedi, bkz. Faz 2 notu) — tek gerçek açık kalan parça bu.
- ✅ **`isLiked`/`isBookmarked`** — sayfa başına tek `IN (...)` sorgusuyla, anonimde `false`.
- ✅ **`excerpt`** — `ThreadMapper.buildExcerpt`, 160 karakter, kelime ortasından kesmiyor.
- ✅ **Rate limiting** — `CommunityRateLimitFilter` (userId bazlı, thread 10/saat, comment 30/saat).
- ✅ **`HotScoreJob`** — 15 dakikada bir, Reddit-tarzı basit formül. `qualityScore` hâlâ hesaplanmıyor (Faz 4 işi).
- ✅ **`commentCount` asimetrisi** — `decrementCommentCount` eklendi, idempotent (zaten silinmiş yoruma tekrar DELETE sayacı düşürmüyor).
- ✅ **Ekstra (öngörülmemiş ama zorunlu) düzeltmeler**: `MovieService`/`SeriesService.existsBySlug` eksikti (community main'de hiç derlenmiyordu), `GlobalExceptionHandler`'a `AccessDeniedException→403` handler'ı eklendi (yoksa owner/moderator kontrolü 500 dönüyordu).

---

## Faz 2 — Community Profile ✅ (daraltılmış kapsamla tamamlandı, `22dd686`)

**Karar (tartışıldı, netleşti): `account/UserProfile` community/'ye TAŞINMADI, olduğu yerde kaldı.** Orijinal planın ayrı bir `CommunityProfileResponse`/yeni entity inşa etme fikri terk edildi — `account/`'ın zaten var olan `UserProfile`/`ProfileStats`/`UserProfileService` altyapısı community/ verisine **service-interface üzerinden bağlandı**, hiç yeni entity yazılmadı.

| Kalem | Durum |
|---|---|
| `ProfileStats.commentCount`/`theoryCount` gerçek veriye bağlanması | ✅ `UserProfileServiceImpl`, `community/ThreadService.countByAuthorIdAndSurface` + `CommentService.countByAuthorId` inject eder |
| `GET /api/users/{username}/profile` (herkese açık) | ✅ `account/PublicAccountController`'a eklendi, `UserService.getIdByUsername` + `UserProfileService.getProfile` — döndürdüğü tip `UserProfileResponse` (account/'ın kendi DTO'su), ayrı bir `CommunityProfileResponse` YOK |
| `GET /api/users/:username/threads` | ❌ yapılmadı — kapsam dışı bırakıldı |
| `GET /api/users/:username/likes` / `bookmarks` | ❌ yapılmadı — kapsam dışı bırakıldı |
| `followerCount`/`isFollowing` (kişi takibi) | ❌ yapılmadı — `account/UserFollow` sadece MOVIE/SERIES/BLOG'u (`SavedItemType`) takip ediyor, kullanıcı-takip-kullanıcı hiç yok, ayrı bir özellik olarak kalmalı |
| `avatarUrl` yazar zenginleştirmesi (Faz 1'in açık kalan parçası) | ❌ hâlâ yapılmadı — `UserProfile.avatarUrl` zaten var ama `AuthorSummary`'ye henüz bağlanmadı, düşük efor bir takip görevi |

**Sonuç**: Faz 2, orijinal plandaki gibi zengin bir "herkese açık community profili" (thread geçmişi, takipçi sayısı) değil, mevcut hesap profilinin (bio/avatar/banner + artık gerçek comment/theory sayaçları) sadece **herkese açık hale getirilmesi** oldu. Thread/like/bookmark geçmişi listeleme ve kişi takibi istenirse ayrı bir iş kalemi.

---

## Faz 3 — Tag Sistemi ✅ (tamamlandı)

| Kalem | Durum |
|---|---|
| `GET /api/community/threads?tag=X` (tag'e göre filtre) | ✅ zaten var, `ThreadSpecificationBuilder.hasIdIn` üzerinden |
| `GET /api/community/tags/trending` | ✅ `ThreadTagRepository.findTrending` — job/cache YOK, on-the-fly JPQL `GROUP BY` (bilinçli basit çözüm), `?window=7d\|30d&limit=` |
| `GET /api/community/tags/:tag/threads` (ayrı path, threadCount ile) | ✅ `TagFollowController` — `ThreadService.list`'e ince bir delege, yeni servis metodu yazılmadı |
| `TagFollow` entity + `POST/DELETE /api/community/tags/:tag/follow`, `GET /api/community/tags/followed` | ✅ `ThreadLike`/`ThreadBookmark` ile aynı idempotent toggle deseninde, `account/`'tan bağımsız |

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

1. ~~**Faz 1 açık eksiklerini kapat**~~ ✅ tamamlandı.
2. ~~**Faz 2 (Community Profile, daraltılmış)**~~ ✅ tamamlandı.
3. **(Düşük efor, ele alınmamış) `avatarUrl` yazar zenginleştirmesi** — `AuthorSummary`'ye `account/UserProfileService`'ten toplu bir `avatarUrl` alanı eklemek, Faz 1'in son açık parçası.
4. ~~**Faz 3 (Tag sistemi)**~~ ✅ tamamlandı.
5. **Faz 4 (Kişiselleştirme)** — en yüksek efor, en son.
