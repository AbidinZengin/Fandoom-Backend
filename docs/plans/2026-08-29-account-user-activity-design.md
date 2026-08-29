# Account & User Activity Management — Tasarım

**Tarih:** 2026-08-29
**Durum:** Onaylandı, implementasyon bekliyor.
**Kaynak:** Kullanıcının getirdiği "Fandoom Account & User Activity Management"
teknik blueprint'i — brainstorming diyaloguyla CLAUDE.md konvansiyonlarına
uyacak şekilde revize edildi (özgün blueprint'te modül yeri, cross-module
doğrulama, migration yaklaşımı, activity log yazma mekanizması gibi kritik
noktalar belirsizdi/mimariyle çelişiyordu).

## Amaç

Hesap paneli: kullanıcı profili (bio/avatar/banner), kişisel listeler
(izleme/okuma/özel koleksiyon), kaydedilen öğeler, beğeni, takip ve
aktivite geçmişi (profil istatistikleri için). Frontend ayrı depoda;
profil sayfası mockup'ı Comments/Theories/Likes stat kartları gösteriyor
— bunlar `community/` modülüne ait kavramlar, `community/` henüz
kurulmadığı için bu sayılar şimdilik hep `0` dönecek ama DTO kontratı
baştan doğru kurulacak.

## Özgün blueprint'ten sapmalar (gerekçeli)

1. **Yeni modül `user/`'a değil, ayrı `account/`'a eklendi.** `user/`
   auth/kimlik (JWT, Role, AdminBootstrap) — farklı bir nedenle değişir.
   Hesap-CRUD'u (profil/liste/saved-item) `person/`'ın Person+Character+Cast
   bundling emsaline uyan ayrı bir modülde. `account/` `user/`'a hiç
   bağımlı değil (`UserService`'i sadece username-lookup için kullanır).
2. **`item_type` kapsamı daraltıldı.** Blueprint `MOVIE, SERIES, BLOG,
   PODCAST, LORE` diyordu — `PODCAST` diye bir modül yok, `LORE` tek tip
   değil (Group/Location/Event). Nihai kapsam: `MOVIE, SERIES, BLOG`
   (`SavedItemType` enum'u — `UserSavedItem`/`UserLike`/`UserFollow`
   arasında paylaşılır).
3. **Cross-module doğrulama, `Cast`'in `subjectType`+`subjectId`
   desenini tekrar kullanıyor.** `MovieService`/`SeriesService`/
   `BlogService` interface'leri inject edilip `existsById` ile doğrulanır
   — blueprint bu konuda tamamen sessizdi.
4. **`user_list_id` NOT NULL.** Blueprint'te nullable'dı ("bağımsız
   bookmark" + "liste üyeliği" iki ayrı kavram gibi duruyordu). Nihai
   model: her `UserSavedItem` bir listenin üyesi. Her kullanıcı için
   `WATCHLIST`/`READLIST` tipinde iki **sistem listesi** lazy oluşturulur
   (silinemez/tipini değiştiremez); "kaydet" butonu bu listelerden birine
   ekler, kullanıcı ayrıca `CUSTOM` liste de açabilir.
5. **Migration dosyası yok.** Blueprint'in `V2__...sql` ismi
   Flyway/Liquibase varsayıyor; projede o araç yok (`ddl-auto=update` +
   gerekirse elle `.sql`). Bunlar yepyeni tablolar (taşınacak veri yok),
   `ddl-auto=update` otomatik açar — migration dosyasına gerek yok.
6. **Activity log yazma mekanizması netleştirildi.** Diğer modüller
   (`blog/`, ileride `community/`) `account/`'un `ActivityLogService`
   interface'ini inject edip doğrudan çağırır (DIP, `CastServiceImpl`
   deseniyle aynı) — event-bus gibi projede hiç kullanılmayan yeni bir
   pattern sokulmadı.
7. **`progress_percentage` sadece `BLOG` için anlamlı.** Platform
   streaming değil (wiki/fandom içerik sitesi) — `MOVIE`/`SERIES` için
   video izleme ilerlemesi diye bir şey yok, sadece watchlist üyeliği var.
8. **`UserActivityLog`'dan `updated_at` kaldırıldı** — append-only log,
   hiçbir satır güncellenmez.
9. **Kapsama sonradan eklenenler:** `UserLike` (MOVIE/SERIES/BLOG,
   beğeni ≠ kaydetme, ayrı entity) ve `UserFollow` (aynı kapsam, "yeni
   içerik geldiğinde bildirim" için takip ilişkisi) — **bildirim
   gönderme mekanizması (email/push, tetikleyici) bu turda kapsam
   dışı**, sadece ilişki verisi kuruluyor.

## Modül yapısı

```
account/
├── entity/     # UserProfile, UserList(+ListType: CUSTOM|WATCHLIST|READLIST),
│               # UserSavedItem(+SavedItemType: MOVIE|SERIES|BLOG),
│               # UserLike(+SavedItemType), UserFollow(+SavedItemType),
│               # UserActivityLog(+ActivityType)
├── repository/ # UserProfileRepository, UserListRepository,
│               # UserSavedItemRepository, UserLikeRepository,
│               # UserFollowRepository, UserActivityLogRepository
├── dto/        # UpdateUserProfileRequest, UserProfileResponse(+ProfileStats),
│               # CreateUserListRequest, UserListSummaryResponse,
│               # UserListDetailResponse, SaveItemRequest,
│               # UserSavedItemResponse, LikeStatusResponse, FollowStatusResponse
├── mapper/     # UserProfileMapper, UserListMapper, UserSavedItemMapper (MapStruct)
├── service/    # UserProfileService, UserListService, UserSavedItemService,
│               # UserLikeService, UserFollowService, ActivityLogService (+ *Impl)
└── controller/ # AccountController (/api/me/**), PublicAccountController
               # (/api/users/{username}/lists/pinned, /api/likes/**/count,
               # /api/follows/**/count)
```

**Bağımsızlık:** `account/` → `MovieService`/`SeriesService`/`BlogService`/
`UserService` interface'lerini inject eder (ID doğrulama + username lookup).
Tersine yön: `blog/` (ve ileride `community/`, `series/`) `account/`'un
`ActivityLogService` interface'ini inject eder. `account/` "yaprak" konumda
— kimse `account/`'un entity/repository'sine dokunmaz, DTO/service interface
dışında hiçbir modül `account/`'a bağımlı değildir.

## Entity'ler & DB şeması

| Entity | Alanlar | Not |
|---|---|---|
| `UserProfile` | `id`, `userId`(unique), `bio`, `avatarUrl`, `bannerUrl`, `accentColor`, `spoilerProtectionEnabled` | Lazy oluşur (ilk `PATCH`'te) |
| `UserList` | `id`, `userId`, `title`, `description`, `coverImageUrl`, `isPublic`, `isPinned`, `listType` | `listType != CUSTOM` ise silinemez/tipi değişemez; max 6 pinli |
| `UserSavedItem` | `id`, `userId`, `itemId`, `itemType`, `userList`(gerçek `@ManyToOne`, aggregate-içi), `progressPercentage`(nullable, sadece BLOG), `notes` | Unique: `(user_id, item_id, item_type, user_list_id)` |
| `UserLike` | `id`, `userId`, `itemId`, `itemType` | Unique: `(user_id, item_id, item_type)`. `UserList`'e bağlı değil |
| `UserFollow` | `id`, `userId`, `itemId`, `itemType` | Unique: `(user_id, item_id, item_type)`. Bildirim tetikleme YOK (ileride ayrı iş) |
| `UserActivityLog` | `id`, `userId`, `activityType`(`READ_BLOG, ADDED_TO_WATCHLIST, LIKED, COMMENTED*, THEORY_CREATED*` — `*` = `community/` gelene kadar hiç yazılmaz), `itemId`, `itemType`, `createdAt` | Append-only, `updatedAt` yok |

`userId` alanları JPA'da düz `Long` (cross-module ID-only), ama DB'de
`app_user(id)` için gerçek `FK ... ON DELETE CASCADE` kalır — bu saf
referans bütünlüğü, `Cast.subjectId`'nin FK'sız kalma nedeni (iki farklı
tabloya işaret edebilmesi) burada geçerli değil, `userId` tek tabloya
işaret ediyor.

## DTO'lar (özet)

Projenin Summary/Detail ayrımı `UserList` için de uygulanıyor:
`UserListSummaryResponse` (liste kartı: id/title/coverImageUrl/listType/
itemCount/isPublic/isPinned) vs `UserListDetailResponse` (+ description +
`PageResponse<UserSavedItemResponse> items`).

`UpdateUserProfileRequest`'te **`userId` yok** — controller
`@AuthenticationPrincipal CustomUserDetails principal` ile JWT'den çözüp
servise ayrı parametre geçer (`AuthController.me()` ile aynı desen). Bu,
`/api/me/**` altındaki tüm endpoint'ler için geçerli — IDOR'u önler.

`itemId`/`itemType` hiçbir DTO'da zenginleştirilmez (ham kalır) — CLAUDE.md
cross-module DTO kuralı, frontend ilgili içeriği zaten kendi endpoint'inden
çekmiş oluyor.

## Servis katmanı

- Cross-module doğrulama: `Cast`'teki `subjectType`/`subjectId` deseninin
  birebir tekrarı — `switch(itemType)` ile ilgili servisin `existsById`'i
  çağrılır, yoksa `InvalidReferenceException`(400).
- Sistem listesi lazy oluşturma: `findByUserIdAndListType(...).orElseGet(
  () -> save(yeni sistem listesi))`. `GET /api/me/lists` ve
  `targetListId == null` olan `POST /api/me/saved-items` bunu tetikler.
- Sistem listesi koruması: yeni exception sınıfı açılmadı, mevcut
  `InvalidReferenceException`(400) yeniden kullanıldı.
- Like/Follow toggle **idempotent**: zaten beğenilmiş/takip edilen öğede
  tekrar `POST` hata fırlatmaz, mevcut durumu döner; `DELETE` no-op.
  Gereksiz 409'lardan kaçınır, frontend toggle butonunu basitleştirir.
- `ActivityLogService` arayüzü: `void record(Long userId, ActivityType type,
  Long itemId, ItemType itemType)` — `blog/` gibi modüller bunu inject eder.

## Endpoint listesi

| Method | Endpoint | Auth |
|---|---|---|
| GET | `/api/me/profile` | AUTH |
| PATCH | `/api/me/profile` | AUTH |
| GET | `/api/me/lists` | AUTH |
| GET | `/api/me/lists/{id}` | AUTH |
| POST | `/api/me/lists` | AUTH |
| PATCH | `/api/me/lists/{id}` | AUTH |
| DELETE | `/api/me/lists/{id}` | AUTH (CUSTOM değilse 400) |
| PATCH | `/api/me/lists/{id}/pin` | AUTH (max 6 pinli, aşarsa 400) |
| POST | `/api/me/saved-items` | AUTH |
| PATCH | `/api/me/saved-items/{id}` | AUTH |
| DELETE | `/api/me/saved-items/{id}` | AUTH |
| POST/DELETE | `/api/me/likes/{itemType}/{itemId}` | AUTH, idempotent toggle |
| GET | `/api/me/likes/{itemType}/{itemId}` | AUTH |
| POST/DELETE | `/api/me/follows/{itemType}/{itemId}` | AUTH, idempotent toggle |
| GET | `/api/me/follows` | AUTH, pagination'lı |
| GET | `/api/likes/{itemType}/{itemId}/count` | PUBLIC |
| GET | `/api/follows/{itemType}/{itemId}/count` | PUBLIC |
| GET | `/api/users/{username}/lists/pinned` | PUBLIC (isPublic && isPinned) |

`SecurityConfig` değişikliği minimal: `/api/me/**` zaten hiçbir path
kuralına girmediği için otomatik `anyRequest().authenticated()`'a düşüyor
— istenen davranışın (AUTH, rol yok) ta kendisi, ek satır gerekmiyor.
Sadece 3 yeni `permitAll` GET path'i eklenecek: `/api/likes/**/count`,
`/api/follows/**/count`, `/api/users/*/lists/pinned`.

## Kapsam dışı (bilinçli olarak ertelendi)

- Bildirim gönderme (email/push, "takip ettiğin yapıma yeni içerik
  geldi" tetikleyicisi) — `UserFollow` tablosu hazır olacak, okuyup
  bildirim üretecek `NotificationService`/scheduled-job ileride.
- `community/` bağlı stats (`commentCount`, `theoryCount`) — DTO'da yer
  ayrıldı, `community/` kurulana kadar hep `0`.
- `ActivityLog`'un ayrı bir modüle çıkarılması — kullanıcı isteğiyle
  şimdilik `account/` içinde, ileride ayrılacak (TODO).
