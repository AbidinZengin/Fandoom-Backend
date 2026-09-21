# 15e. Portal — API Sözleşmesi (FE referansı)

Kaynak: gerçek kod (`community/controller|service|dto|repository`, `common/config/SecurityConfig`, `common/exception/GlobalExceptionHandler`, `portal_migration.sql`). Tasarım niyeti: Portal katmanı sözleşme metni. Kod ile niyet çeliştiğinde KOD esas alındı; farklar sonda "Sapmalar ve varsayımlar" başlığında.

Depoda önceden yazılmış bir API sözleşme dokümanı/formatı yok (`docs/plans/*` tasarım notlarıdır), bu yüzden biçim: uç başına yol, metot, auth, parametreler (tip / zorunluluk / sınır), response alanları, hata kodları, örnek JSON.

---

## 0. Genel kurallar

### 0.1 Temel

- Base path: `/api`. Portal uçları `/api/community/portals/**`, `/api/me/portals`, admin `/api/admin/community/portals`.
- Auth: `Authorization: Bearer <JWT>`. Geçersiz/süresi dolmuş/revoke edilmiş token, GET public uçlarda hata vermez, istek ANONİM işlenir (`isMember=false`, `isLiked=false`...). Auth gerektiren uçlarda 401.
- Tarih alanları (`createdAt`, `updatedAt`, `timestamp`): zaman dilimsiz ISO-8601 `LocalDateTime` (ör. `"2026-09-21T14:03:11.482913"`).
- JSON alan adları record bileşen adlarıyla birebir (`isMember`, `isLiked`, `isBookmarked` dahil; `is` öneki KALIR).
- Bilinmeyen `sort` değerleri hata vermez: `/portals` için `trending`, thread/feed için `hot` sayılır.

### 0.2 Locale (`name` / `description`)

- Yalnız `Accept-Language` başlığı okunur (`AcceptHeaderLocaleResolver`, desteklenenler: `tr`, `en`). Başlık yoksa veya başka bir dilse `en`. **`?locale=` query parametresi YOKTUR.**
- `name`/`description` seçilen dile göre çözülür; seçilen alan boş/null ise DİĞER dile düşer (en -> tr, tr -> en).
- Thread yanıtlarındaki `portal.name` de aynı kuralla çözülür. Cache anahtarı dili içerir; dil karışmaz.
- Admin uçları çözümleme yapmaz: ham `nameTr/nameEn/descriptionTr/descriptionEn` döner.
- `sort=alpha` (portal dizini) sıralamayı `Accept-Language`'e göre `nameTr` (tr) veya `nameEn` (diğer) üzerinden yapar.

### 0.3 Sayfalama zarfları

`PageResponse<T>` (offset):

```json
{ "content": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0, "last": true }
```

Query: `page` (0 tabanlı, varsayılan 0), `size` (varsayılan 20). `KeysetPageResponse<T>` (cursor):

```json
{ "content": [], "hasNext": true, "nextCursor": "MC4xMjN8NDI" }
```

`nextCursor` opaktır; bir sonraki istekte `cursor=` olarak aynen gönderilir, ilk istekte gönderilmez. `size` 1..50'ye kırpılır (hata vermez). Cursor bozuk veya farklı `sort` ile üretilmişse 400 (`Geçersiz cursor`). Portal listelerinde (`/portals`) `size` 1..50'ye kırpılır. `/threads`, feed (offset) ve admin listede kırpma yoktur (Spring varsayılan üst sınırı: 2000).

### 0.4 Hata gövdesi (`ApiErrorResponse`)

```json
{
  "timestamp": "2026-09-21T14:03:11.482913",
  "status": 400,
  "error": "Bad Request",
  "message": "Doğrulama hatası",
  "path": "/api/community/threads",
  "fieldErrors": ["portalSlug: must not be blank"]
}
```

`fieldErrors` yalnızca Bean Validation (400) hatasında dolu, aksi halde `null`. Format `"<alan>: <mesaj>"`; mesaj metni `Accept-Language`'e göre değişebilir, FE mesaj metnine değil `status` + `fieldErrors` anahtarlarına bağlanmalı. Aynı gövde 401/403/429 için de kullanılır. `message` Türkçedir, çevrilmez. Tüm hata tablosu bölüm 9'da.

### 0.5 Enum'lar

| Enum | Değerler | Anlam |
|---|---|---|
| `PortalStatus` | `ACTIVE`, `ARCHIVED`, `HIDDEN` | ACTIVE: normal. ARCHIVED: dizinde listelenir, okunur; YAZMA KAPALI: yeni thread yazılamaz, thread'ler (moderatör/admin dışında) düzenlenemez (PATCH), yorum eklenemez, like/bookmark eklenemez, yeni üye alınmaz, thread taşınamaz — hepsi 400. Serbest kalanlar: DELETE (thread/yorum), unlike/unbookmark, portaldan ayrılma. HIDDEN: hiçbir public uçta görünmez; portal ve içindeki thread'ler 404, yalnız admin listesinde vardır. |
| `PortalPostingPolicy` | `OPEN`, `STAFF_ONLY` | OPEN: girişli herkes thread açar. STAFF_ONLY: yalnız `MODERATOR`/`ADMIN` (`EDITOR` DEĞİL). |
| `ThreadSurface` | `DISCUSSION`, `THEORY`, `FAN_ART` | Büyük harf, birebir. Hatalı değer query'de 400. |
| `ThreadStatus` (iç) | `PUBLISHED`, `DELETED`, `HIDDEN` | API'de dönmez; public okuma yalnız `PUBLISHED`. |
| `ThreadMediaType` | `IMAGE`, `VIDEO` | |
| `PortalProductionType` (iç) | `MOVIE`, `SERIES` | API'de dönmez; `productionSlugs` düz string listesidir. |
| feed/thread `sort` | `hot` (varsayılan), `new`, `top` | `top` = `likeCount` azalan. |
| portal `sort` | `trending` (varsayılan), `members`, `new`, `alpha` | |
| `scope` | `all` (varsayılan), `joined` | Büyük/küçük harf duyarsız; başka değer 400. |

### 0.6 Rate limit (userId bazlı, tek instance in-memory, 1 saatlik pencere)

`POST /api/community/threads`: 10/saat. Yorum oluşturma (`POST /threads/{id|slug}/comments` ve `POST /api/community/comments`): 30/saat, hepsi AYNI bucket'ı paylaşır (id ve slug varyantı dahil). Portal `join` (POST) ve `leave` (DELETE): 60/saat, ikisi AYNI bucket'ı paylaşır. Aşımda 429 (`ApiErrorResponse`). Filtre doğrulamadan önce çalışır: geçersiz istekler de kotayı tüketir.

---

## 1. Tipler

### 1.1 `PortalSummaryResponse` (dizin, `/me/portals`)

| Alan | Tip | Not |
|---|---|---|
| `id` | long | |
| `slug` | string | Değişmez. `^[a-z0-9]+(-[a-z0-9]+)*$`, 3-50 |
| `name` | string | Locale'e göre çözülmüş |
| `description` | string \| null | Locale'e göre çözülmüş; ikisi de boşsa null |
| `bannerUrl` | string \| null | Cloudinary URL, <=500 |
| `iconUrl` | string \| null | Cloudinary URL, <=500 |
| `accentColor` | string \| null | Opsiyonel vurgu rengi `#RRGGBB` (6 haneli hex). Yoksa `null` -> FE nötr koyu gradyana düşer. Gradyan/okunabilirlik (color-mix) FE'nin işi; backend yalnız saklar. `PortalRefResponse`'ta (thread yanıtlarındaki `portal`) YOK |
| `memberCount` | int | Denormalize, >=0 |
| `threadCount` | int | Denormalize, yalnız PUBLISHED thread'ler |
| `isMember` | boolean | Anonimde `false`; `/me/portals`'ta hep `true` |
| `postingPolicy` | `PortalPostingPolicy` | |
| `status` | `PortalStatus` | Public uçlarda yalnız `ACTIVE`/`ARCHIVED` görülür |
| `productionSlugs` | string[] | Bağlı yapım slug'ları, alfabetik; yapımsız portalda `[]` |

### 1.2 `PortalDetailResponse`

`PortalSummaryResponse` alanlarının hepsi (aynı sırada) + `createdAt` (LocalDateTime).

### 1.3 `PortalRefResponse` (thread yanıtlarındaki `portal`)

`{ "slug": string, "name": string }` — `name` locale'e göre.

### 1.4 `ThreadSummaryResponse` / `ThreadDetailResponse` — `portal` alanı

Mevcut alanlar (adlar/şekiller) DEĞİŞMEDİ; sonuna `portal` eklendi. Yeni alan: `portal: { slug, name }`. Bu alan tüm thread yanıtlarında bulunur: `GET /feed`, `/feed/cursor`, `/threads`, `/portals/{slug}/feed`, `GET /threads/{id|slug}`, `POST /threads`, `PATCH /threads/...`. Normal koşulda hiçbir zaman null değildir (bkz. Sapmalar #17).

`ThreadSummaryResponse` alan sırası: `id, slug, surface, title, excerpt, imageUrl, media[], spoilerFlagged, authorId, author{id,username,avatarUrl}, productionSlug, likeCount, commentCount, bookmarkCount, isLiked, isBookmarked, tags[], createdAt, portal`.

`ThreadDetailResponse`: `id, slug, surface, title, body, imageUrl, media[], spoilerFlagged, authorId, author, productionSlug, likeCount, commentCount, bookmarkCount, isLiked, isBookmarked, tags[], createdAt, updatedAt, portal`.

`media[]` = `{ "type": "IMAGE"|"VIDEO", "url": string, "position": int }`. `imageUrl` geriye uyumluluk içindir (ilk IMAGE); yeni kod `media` kullanmalı.

Örnek (`ThreadDetailResponse`):

```json
{
  "id": 4127,
  "slug": "the-three-eyed-raven-theory",
  "surface": "THEORY",
  "title": "The three-eyed raven theory",
  "body": "…",
  "imageUrl": null,
  "media": [],
  "spoilerFlagged": true,
  "authorId": 12,
  "author": { "id": 12, "username": "arya", "avatarUrl": null },
  "productionSlug": "game-of-thrones",
  "likeCount": 3,
  "commentCount": 1,
  "bookmarkCount": 0,
  "isLiked": false,
  "isBookmarked": false,
  "tags": ["bran", "theory"],
  "createdAt": "2026-09-21T14:03:11.482913",
  "updatedAt": "2026-09-21T14:03:11.482913",
  "portal": { "slug": "westeros", "name": "Westeros" }
}
```

---

## 2. Portal okuma uçları (public, GET)

### 2.1 `GET /api/community/portals` — Portal dizini

- Auth: yok (opsiyonel; token varsa `isMember` çözülür).
- Query:

| Param | Tip | Zorunlu | Varsayılan | Sınır / Not |
|---|---|---|---|---|
| `sort` | string | hayır | `trending` | `trending` \| `members` \| `new` \| `alpha`; başka değer = `trending` |
| `q` | string | hayır | — | Ad araması: `name_tr` VE `name_en` üzerinde büyük/küçük harf duyarsız "içerir" (LIKE); trim edilir; `%`, `_`, `!` escape (kullanıcı wildcard veremez). Boş/boşluk = filtre yok. En fazla 100 karakter, aşarsa 400 |
| `page` | int | hayır | 0 | |
| `size` | int | hayır | 20 | 1..50'ye kırpılır |

- Response 200: `PageResponse<PortalSummaryResponse>`. `ACTIVE` + `ARCHIVED` listelenir, `HIDDEN` hiç görünmez.
- Sıralama:
  - `trending`: skor = `3 * (son 7 günde açılan PUBLISHED thread) + (son 7 günde PUBLISHED thread'lere yazılan PUBLISHED yorum)`; eşitlikte `sortOrder` artan, sonra `id` artan. Kayan 7 günlük pencere (`now - 7 gün`), her istekte hesaplanır (job/cache yok). Formül tek yerde: `PortalQueryServiceImpl`.
  - `members`: `memberCount` azalan, `id` artan.
  - `new`: `createdAt` azalan, `id` azalan.
  - `alpha`: ad artan (dile göre TR/EN), `id` artan.
- Hatalar: 400 (`q` > 100 karakter).

```json
{
  "content": [{
    "id": 2, "slug": "westeros", "name": "Westeros",
    "description": "Shared room for Game of Thrones and House of the Dragon fans.",
    "bannerUrl": null, "iconUrl": null, "accentColor": "#C0392B",
    "memberCount": 128, "threadCount": 412, "isMember": false,
    "postingPolicy": "OPEN", "status": "ACTIVE",
    "productionSlugs": ["game-of-thrones", "house-of-the-dragon"]
  }],
  "page": 0, "size": 20, "totalElements": 9, "totalPages": 1, "last": true
}
```

### 2.2 `GET /api/community/portals/{slug}` — Portal detayı

- Auth: yok (opsiyonel, `isMember` için).
- Path: `slug` (string).
- Response 200: `PortalDetailResponse` (`ARCHIVED` dahil dönebilir).
- Hatalar: 404 (yok veya `HIDDEN`).

### 2.3 `GET /api/community/portals/{slug}/feed` — Portal akışı

- Auth: yok (opsiyonel; `isLiked/isBookmarked` için).
- `GET /api/community/feed?portal={slug}` ile AYNI sonucu (aynı servis çağrısı) döner. Bu yol `productionSlug`, `tags`, `scope` ve cursor varyantı SUNMAZ; bunlar için `/feed` kullanın.
- Query: `surface` (`ThreadSurface`, ops.), `sort` (`hot`|`new`|`top`, varsayılan `hot`), `page`, `size` (varsayılan 20).
- Response 200: `PageResponse<ThreadSummaryResponse>` (yalnız PUBLISHED thread).
- Hatalar: 404 (portal yok/`HIDDEN`), 400 (`surface` geçersiz).

---

## 3. Feed

### 3.1 `GET /api/community/feed`

- Auth: `scope=all` için yok (opsiyonel); `scope=joined` için ZORUNLU (anonim = 401).
- Query:

| Param | Tip | Zorunlu | Varsayılan | Not |
|---|---|---|---|---|
| `surface` | `ThreadSurface` | hayır | tümü | |
| `portal` | string (portal slug) | hayır | tümü | Yok/`HIDDEN` = 404. `scope=joined` ile birlikte: portal üyelikte değilse sonuç BOŞ sayfa (404 değil) |
| `productionSlug` | string | hayır | — | Thread'in `productionSlug`'ı ile eşitlik |
| `tags` | string, tekrarlı (`?tags=a&tags=b`) | hayır | — | OR mantığı; normalize edilir (slugify) |
| `sort` | string | hayır | `hot` | `hot`\|`new`\|`top` |
| `scope` | string | hayır | `all` | `all`\|`joined`; başka değer 400 |
| `page`, `size` | int | hayır | 0 / 20 | |

- Filtreler birbiriyle AND ile birleşir. `portal` verilmediğinde `HIDDEN` portalların thread'leri sonuçtan çıkarılır.
- `scope=joined`: yalnız kullanıcının üye olduğu, `HIDDEN` olmayan portalların (ARCHIVED dahil) thread'leri; hiç üyelik yoksa boş sayfa.
- Response 200: `PageResponse<ThreadSummaryResponse>`.
- Hatalar: 401 (`scope=joined` + anonim), 400 (`scope`/`surface` geçersiz), 404 (`portal` yok/HIDDEN).

Örnek: `GET /api/community/feed?portal=westeros&surface=THEORY&sort=hot`

### 3.2 `GET /api/community/feed/cursor`

Aynı filtreler (`surface`, `portal`, `productionSlug`, `tags`, `sort`, `scope`) + `cursor` (ops.) + `size` (varsayılan 20, 1..50'ye kırpılır). Sayfa parametresi yok. Response 200: `KeysetPageResponse<ThreadSummaryResponse>`. Sıralama daima `(sıralama alanı DESC, id ASC)`. Hatalar: `/feed` ile aynı + 400 (bozuk cursor).

`scope=joined` cursor yolunda da 401 kuralı geçerlidir (SecurityConfig her iki yolu da kapsar).

Not: `GET /api/community/threads` (`portal`, `productionSlug`, `tags`, `surface`, `sort`, `page`, `size`) aynı servisi çağırır; `scope` YOKTUR.

---

## 4. Thread uçları (id tabanlı — birincil)

Tüm `{id}` path değişkenleri `\d+` regex'lidir: yalnız rakamlardan oluşan segment HER ZAMAN id olarak yorumlanır (bkz. 7.2).

### 4.1 `GET /api/community/threads/{id}`

- Auth: yok (opsiyonel).
- Response 200: `ThreadDetailResponse` — `id`, `slug`, `portal.slug` her zaman dolu.
- 404 durumları: id yok; thread `DELETED` veya `HIDDEN`; thread'in portalı `HIDDEN`.
- Anonim istekler cache'lenebilir (kullanıcıya özel alan yok); girişli istek hep tazedir. Başlık değişimi sonrası eski id çalışır, yanıttaki `slug` yeni slug'dır.
- id, `Long` sınırını aşarsa (19+ hane) 400 (tip uyuşmazlığı).

### 4.2 `POST /api/community/threads` — Oluştur

- Auth: gerekli (herhangi bir giriş yapmış rol). Rate limit: 10/saat.
- Body `ThreadRequest`:

| Alan | Tip | Zorunlu | Sınır |
|---|---|---|---|
| `portalSlug` | string | EVET | not blank, <=50 |
| `surface` | `ThreadSurface` | evet | |
| `title` | string | evet | 10..200 (not blank) |
| `body` | string | hayır | <=10000 |
| `productionSlug` | string | hayır | <=255. Verilirse hem geçerli bir Movie/Series slug'ı hem SEÇİLEN PORTALIN yapımlarından biri olmalı |
| `tags` | string[] | hayır | <=10 eleman, her biri not blank <=100; normalize edilir |
| `media` | `{type,url}[]` | hayır | <=6; `url` <=500, projenin Cloudinary'sine ait olmalı |
| `spoilerFlagged` | boolean | hayır | varsayılan false |
| `imageUrl` | string | hayır | DEPRECATED; yalnız `media` boş/yoksa `media[IMAGE]` gibi işlenir |

- `slug` sunucuda başlıktan üretilir (`SlugGenerator`, çakışmada `-2`, `-3`); client göndermez.
- Response 201: `ThreadDetailResponse` (portal dahil).
- Sunucu doğrulama sırası: 401 -> 429 -> Bean Validation (400 + `fieldErrors`) -> portal (404/400/403) -> productionSlug (400) -> medya (400).
- Portal kuralları:

| Durum | Kod |
|---|---|
| `portalSlug` eksik/boş | 400, `fieldErrors: ["portalSlug: ..."]` |
| Portal yok veya `HIDDEN` | 404 |
| Portal `ARCHIVED` | 400 (yazma kapalı) |
| Portal `STAFF_ONLY` ve kullanıcı `MODERATOR`/`ADMIN` değil | 403 |
| `productionSlug` geçersiz | 400 |
| `productionSlug` portalın yapımı değil (yapımsız portal dahil, ör. `genel-sohbet`) | 400 |

- Yan etki: portalın `threadCount`'u +1.

```json
{
  "portalSlug": "westeros",
  "surface": "THEORY",
  "title": "The three-eyed raven theory",
  "body": "…",
  "productionSlug": "game-of-thrones",
  "tags": ["bran"],
  "spoilerFlagged": true
}
```

### 4.3 `PATCH /api/community/threads/{id}` — Güncelle / taşı

- Auth: gerekli; sahibi veya `MODERATOR`/`ADMIN` (aksi 403).
- Body `ThreadPatchRequest` (tüm alanlar opsiyonel, `null`/eksik = değişmedi):

| Alan | Tip | Sınır / Not |
|---|---|---|
| `title` | string | 10..200. Değişirse slug YENİDEN üretilir (id URL'i kırılmaz) |
| `body` | string | <=10000 |
| `spoilerFlagged` | boolean | |
| `tags` | string[] | <=10; `[]` = hepsi silinir |
| `media` | `{type,url}[]` | <=6; `[]` = hepsi silinir; dolu = tamamen değiştir |
| `imageUrl` | string | DEPRECATED (yalnız `media` yokken); `""` = medya temizle |
| `portalSlug` | string | <=50. Portal TAŞIMA: kurallar aşağıda |

- Taşıma kuralları (`portalSlug` gönderildiğinde):
  - `MODERATOR`/`ADMIN`: hedef yok/`HIDDEN` -> 404; hedef `ARCHIVED` -> 400; hedef mevcut portalla aynıysa no-op. Taşıma sonrası eski portalın `threadCount`'u -1, yenisinin +1 (tek transaction; yalnız thread `PUBLISHED` ise).
  - Sahibi (moderatör değil): `portalSlug` MEVCUT portalın slug'ıyla aynıysa (büyük/küçük harf duyarsız) no-op; farklıysa 403 (hedef var mı yok mu bakılmaz).
  - Taşıma `productionSlug`'ı değiştirmez ve yeniden doğrulamaz (bkz. Sapmalar #9).
- Response 200: `ThreadDetailResponse`. FE, yanıttaki `slug` ve `portal.slug` ile URL'i güncellemelidir (bölüm 7).
- Hatalar: 401, 403, 404 (id yok), 400.

### 4.4 `DELETE /api/community/threads/{id}`

Auth: sahibi veya `MODERATOR`/`ADMIN`. ARCHIVED portalda de serbesttir (sahip kendi içeriğini silebilir). Portalı `HIDDEN` ise sahip için 404. 204 gövdesiz. Soft-delete (`status=DELETED`); `threadCount` yalnız thread PUBLISHED idiyse -1 (tekrar DELETE sayacı düşürmez, idempotent). 404: id yok.

### 4.5 Like / Bookmark (id)

| Metot / Yol | Auth | 200 gövdesi |
|---|---|---|
| `POST /api/community/threads/{id}/like` | gerekli | `{ "liked": true, "likeCount": int }` |
| `DELETE /api/community/threads/{id}/like` | gerekli | `{ "liked": false, "likeCount": int }` |
| `POST /api/community/threads/{id}/bookmark` | gerekli | `{ "bookmarked": true, "bookmarkCount": int }` |
| `DELETE /api/community/threads/{id}/bookmark` | gerekli | `{ "bookmarked": false, "bookmarkCount": int }` |

İdempotent (tekrar POST/DELETE hata vermez, mevcut durumu döner). 404: thread yok / `DELETED` / `HIDDEN` / portalı `HIDDEN`. 401: anonim. 400: portalı `ARCHIVED` ise `POST` (like/bookmark ekleme) reddedilir; `DELETE` (unlike/unbookmark) serbesttir.

### 4.6 Yorumlar (id)

| Metot / Yol | Auth | Not |
|---|---|---|
| `GET /api/community/threads/{id}/comments` | yok (opsiyonel) | `sort` (`new` varsayılan \| `hot`), `page`, `size` (20) -> `PageResponse<CommentResponse>` |
| `GET /api/community/threads/{id}/comments/cursor` | yok (opsiyonel) | `sort`, `cursor`, `size` (20, 1..50) -> `KeysetPageResponse<CommentResponse>` |
| `POST /api/community/threads/{id}/comments` | gerekli | Body `{ "body": string 2..2000, "spoilerFlagged": bool, "parentId": long? }` -> 201 `CommentResponse`. Rate limit 30/saat |

- 404: thread yok / PUBLISHED değil / portalı `HIDDEN` (üç uç için de).
- 400: thread portalı `ARCHIVED` ise yorum eklenemez (merkezi `POST /api/community/comments` dahil); yorum like'ı da aynı şekilde reddedilir (unlike serbest).
- POST hataları: 400 (`parentId` başka konuya ait / bir yanıta yanıt = 2 seviye sınırı), 404 (`parentId` yok).
- `sort=hot` (yorum) = `likeCount` azalan; başka herhangi bir değer = `createdAt` azalan.

`CommentResponse`: `id, subjectType, subjectId, threadId, parentId, body, spoilerFlagged, authorId, author{id,username,avatarUrl}, likeCount, isLiked, replyCount, replies[] (yalnız üst seviyede, en fazla 3), createdAt`. `subjectType`/`subjectId` birincil, `threadId` geriye uyumluluk (THREAD iken `subjectId` ile aynı). Silinmiş yorumun `body`'si `"[silindi]"`.

---

## 5. Deprecated slug uçları

Aşağıdaki uçlar AYNEN çalışır (davranış değişmedi) ama Java `@Deprecated` işaretlidir (springdoc'ta `deprecated: true` görünür). Slug başlıkla değiştiği için yeni kod id varyantını kullanmalı. Slug uçları yalnız SAYISAL OLMAYAN slug'ları yakalar (bölüm 7.2).

| Deprecated | Yerine |
|---|---|
| `GET /api/community/threads/{slug}` | `GET /threads/{id}` |
| `PATCH /api/community/threads/{slug}` | `PATCH /threads/{id}` |
| `DELETE /api/community/threads/{slug}` | `DELETE /threads/{id}` |
| `POST\|DELETE /api/community/threads/{slug}/like` | `.../{id}/like` |
| `POST\|DELETE /api/community/threads/{slug}/bookmark` | `.../{id}/bookmark` |
| `GET /api/community/threads/{slug}/comments` | `.../{id}/comments` |
| `GET /api/community/threads/{slug}/comments/cursor` | `.../{id}/comments/cursor` |
| `POST /api/community/threads/{slug}/comments` | `.../{id}/comments` |

Slug uçlarının portal davranışı id uçlarıyla aynıdır (HIDDEN portal = 404; PATCH'te taşıma kuralı aynı).

Değişmeyenler (portaldan etkilenmeyen): `DELETE /api/community/comments/{id}`, `POST|DELETE /api/community/comments/{id}/like`, merkezi `GET|POST /api/community/comments[/cursor]` (`subjectType`+`subjectId`).

---

## 6. Üyelik (hepsi AUTH)

### 6.1 `POST /api/community/portals/{slug}/join`

- Auth: gerekli. Gövde yok.
- Response 200: `{ "member": true, "memberCount": int }` (işlem sonrası değerler).
- Idempotent: zaten üyeyken tekrar çağrı `memberCount`'u artırmaz, aynı yanıtı döner. Sayaç yalnız gerçekten yeni satır eklenince artar (eşzamanlı çağrılarda da).
- Hatalar: 401; 404 (portal yok/`HIDDEN`); 400 (portal `ARCHIVED` ve kullanıcı henüz üye değil). Zaten üye olan kullanıcı ARCHIVED portala join çağırırsa 200 döner (üyelik korunur).

### 6.2 `DELETE /api/community/portals/{slug}/join`

- Response 200: `{ "member": false, "memberCount": int }`. Idempotent: üye değilken çağrı sayacı düşürmez. `ARCHIVED` portaldan ayrılmak serbesttir.
- Hatalar: 401; 404 (portal yok/`HIDDEN` — HIDDEN portaldan ayrılma da 404).

### 6.3 `GET /api/me/portals` — Portallarım

- Auth: gerekli.
- Query: `sort` (ops.): `activity` = son 7 günlük aktivite (trending formülü) azalan, eşitlikte joinedAt azalan; başka değer/eksik = `joinedAt` azalan (eşitlikte portal id artan).
- Response 200: `PortalSummaryResponse[]` — DÜZ LİSTE (sayfalama yok), her öğede `isMember=true`. `ARCHIVED` görünür, `HIDDEN` görünmez. Üyelik yoksa `[]`.

---

## 7. Hibrit URL kuralı, numeric slug

### 7.1 Post URL'i

FE rotası: `/portal/{portalSlug}/post/{id}-{post-slug}`. Backend sorguyu YALNIZ `id` ile yapar (`GET /api/community/threads/{id}`); slug süstür.

FE algoritması:
1. Segmentin başındaki rakam dizisini id olarak al: `^(\d+)(?:-|$)`. (Post slug'ı rakamla başlayabilir — ör. `4127-12-monkeys-theory` — id = `4127`.)
2. `GET /api/community/threads/{id}`.
3. Yanıttaki `portal.slug` ve `slug` ile URL'dekileri karşılaştır. Farklıysa (başlık değişti, thread başka portala taşındı) `history.replace` ile kanonik URL'e geç: `/portal/{response.portal.slug}/post/{response.id}-{response.slug}`. Backend yönlendirme (301/302) YAPMAZ.
4. 404 -> "bulunamadı" sayfası (silinmiş, gizli ve HIDDEN portaldaki thread'ler dahil, ayırt edilemez).

`PATCH`/`POST` yanıtları da güncel `slug` ve `portal.slug` içerir; düzenleme sonrası URL bunlarla yenilenmeli.

### 7.2 Numeric-slug notu

Yeni thread'lerin slug'ı ARTIK saf-sayısal olmaz: başlıktan üretilen slug yalnız rakamsa `-t` eki alır (başlık `1234567890` -> `1234567890-t`, çakışırsa `-t-2`). Bu yüzden:
- `/api/community/threads/{segment}` içinde `segment` yalnız rakamsa HER ZAMAN id'dir; deprecated slug uçları `(?!\d+$).+` regex'iyle rakam-only segmentleri hiç yakalamaz (savunma katmanı olarak duruyor).
- Bu düzeltmeden ÖNCE oluşturulmuş saf-sayısal slug'lı eski thread'ler (varsa) yalnız id ile erişilebilir; eski slug-tabanlı link o thread'i DEĞİL, id=<sayı> olanı arar. Yeni kayıtlarda bu durum oluşmaz. Slug başlık değişince yeniden üretilir, kural aynen uygulanır.
- Slug'ın rakamla BAŞLAMASI sorun değildir (`12-monkeys`), yalnız tamamı rakamsa `-t` eki devreye girer.

---

## 8. Admin uçları (yalnız `ADMIN`; tümü `/api/admin/community/portals`)

`ADMIN` olmayan giriş yapmış kullanıcı: 403; anonim: 401. Silme ucu YOKTUR (kaldırmak = `ARCHIVED`/`HIDDEN`). Görsel alanları yalnız URL saklar: önce `POST /api/media/images` ile yüklenir, dönen `url` gönderilir; URL projenin Cloudinary hesabına ait bir görsel olmalıdır (aksi 400; `CLOUDINARY_URL` set değilse boş olmayan her URL 400 alır).

### 8.1 `GET /api/admin/community/portals`

Query: `page`, `size` (20). Sıra sabit: `sortOrder` artan, `id` artan (client `sort` yok sayılır). `HIDDEN` dahil hepsi. Response 200: `PageResponse<AdminPortalResponse>`.

`AdminPortalResponse`: `id, slug, nameTr, nameEn, descriptionTr, descriptionEn, bannerUrl, iconUrl, accentColor, status, postingPolicy, sortOrder, memberCount, threadCount, productionSlugs[], createdAt, updatedAt`.

### 8.2 `POST /api/admin/community/portals` — Oluştur

Body `PortalCreateRequest`:

| Alan | Tip | Zorunlu | Sınır / Varsayılan |
|---|---|---|---|
| `slug` | string | evet | 3..50, `^[a-z0-9]+(-[a-z0-9]+)*$`, sonradan DEĞİŞMEZ |
| `nameTr` | string | evet | 2..60 |
| `nameEn` | string | evet | 2..60 |
| `descriptionTr` | string | hayır | <=300; boş/boşluk = null |
| `descriptionEn` | string | hayır | <=300 |
| `bannerUrl` | string | hayır | <=500, sahip olunan görsel |
| `iconUrl` | string | hayır | <=500 |
| `accentColor` | string | hayır | `#RRGGBB` (büyük/küçük harf serbest, olduğu gibi saklanır); `""`/null = renk yok. Geçersiz -> 400 (`fieldErrors`) |
| `postingPolicy` | enum | hayır | varsayılan `OPEN` |
| `status` | enum | hayır | varsayılan `ACTIVE` |
| `productionSlugs` | string[] | hayır | <=50 eleman, her biri <=280; tekrarlar tekilleştirilir; `[]`/null = yapımsız portal |
| `sortOrder` | int | hayır | varsayılan 0 |

Response 201: `AdminPortalResponse`. Hatalar: 400 (doğrulama `fieldErrors`; geçersiz görsel URL'i; `productionSlug` Movie/Series'te yok), 409 (slug zaten var; yapım zaten BAŞKA bir portala bağlı — bir yapım en fazla bir portala bağlıdır).

### 8.3 `PATCH /api/admin/community/portals/{slug}`

Body `PortalUpdateRequest`: create ile aynı alanlar; hepsi opsiyonel, `null` = değişmedi. Ek kurallar:
- `slug` gönderilirse mevcut slug ile aynı olmalı (aksi 400 `Portal slug'ı değiştirilemez`).
- `nameTr/nameEn`: 2..60. `descriptionTr/En`: `""` = temizle. `bannerUrl/iconUrl`: `""` = görseli kaldır. `accentColor`: null = değişmedi, `""` = rengi kaldır, dolu = `#RRGGBB` (aksi 400).
- `productionSlugs`: `null` = değişmedi, `[]` = tüm bağlar silinir, dolu = TAMAMEN değiştirir (fark tabanlı: yalnız çıkanlar silinir, yalnız yeniler eklenir).
- `status`: `HIDDEN`/`ACTIVE`/`ARCHIVED` doğrudan ayarlanabilir (HIDDEN'ı açmanın tek yolu budur).

Response 200: `AdminPortalResponse`. Hatalar: 404 (portal yok), 400, 409.

### 8.4 `POST /api/admin/community/portals/{slug}/archive` ve `/unarchive`

Gövde yok. `archive` -> `status=ARCHIVED`, `unarchive` -> `status=ACTIVE`. Response 200: `AdminPortalResponse`. Idempotent. Hatalar: 404 (yok), 400 (portal `HIDDEN` ise; önce PATCH ile `status` değiştirilmeli).

Her admin yazması thread liste/detay cache'lerini temizler; `portal.name` ve HIDDEN filtresi anında yansır.

---

## 9. Hata kodu tablosu (portal + thread uçları)

| Kod | Ne zaman |
|---|---|
| 400 | Bean Validation (`fieldErrors` dolu): `portalSlug` eksik/boş, `title` 10..200, admin alan sınırları vb. |
| 400 | `InvalidReferenceException` (`fieldErrors` null): geçersiz `scope`, `q` > 100, bozuk cursor, ARCHIVED portala thread yazma / thread taşıma / yeni üyelik / ARCHIVED portalda thread PATCH (moderatör hariç), yorum ekleme, like, bookmark, yorum like, `productionSlug` geçersiz veya portalın yapımı değil, admin `slug` değişikliği, admin `HIDDEN` portal archive/unarchive, geçersiz görsel URL, `parentId` kuralları, >10 tag, geçersiz medya |
| 400 | Query'de tip uyuşmazlığı: `surface=foo`, `scope` dışı enum'lar, sığmayan `{id}` (`Geçersiz parametre değeri: ...`) |
| 401 | Token yok/geçersiz + auth gerektiren uç: `POST/PATCH/DELETE` thread, like/bookmark, yorum yazma, `join/leave`, `/api/me/portals`, `GET /feed[/cursor]?scope=joined`. Gövde: `Kimlik doğrulama gerekli: lütfen giriş yapın` |
| 403 | STAFF_ONLY portala normal kullanıcı thread'i; thread'i sahibi/moderatör olmayan biri PATCH/DELETE; sahibinin portal taşıma denemesi; `/api/admin/community/**` için `ADMIN` olmayan kullanıcı. Gövde mesajı genellikle `Bu işlem için yetkiniz yok` |
| 404 | Portal yok veya `HIDDEN` (tüm public/üyelik uçları); thread yok / `DELETED` / `HIDDEN` / portalı `HIDDEN` (`GET`, like, bookmark, yorum uçları); admin uçlarında portal yok; taşıma hedefi yok/HIDDEN |
| 409 | Admin: slug zaten var, yapım başka portala bağlı; DB unique ihlali son çare (`Veri bütünlüğü ihlali ...`) |
| 400 | Bozuk JSON / okunamayan istek gövdesi / bilinmeyen enum değeri (`İstek gövdesi okunamadı veya geçersiz`) |
| 405 | Yol için HTTP metodu desteklenmiyor (`Allow` başlığı döner) |
| 415 | Desteklenmeyen içerik tipi |
| 429 | Thread oluşturma 10/saat, yorum oluşturma 30/saat, portal join/leave 60/saat aşıldı |
| 500 | Beklenmeyen hata (`Beklenmeyen bir hata oluştu`) — bkz. Sapmalar #16 |

---

## 10. Sapmalar ve varsayımlar

Spec (portal-spec.md) ile kod arasındaki farklar; her birinde KOD esas alındı. (Doküman kod okunarak yazıldı; 2026-09-21'de güvenlik düzeltmeleri + A–E kararlarından sonra güncellendi ve tam test paketi koşuldu.)

1. **`?locale=` yok.** Spec "`?locale=`/Accept-Language" diyor; kodda yalnız `Accept-Language` (`AcceptHeaderLocaleResolver`, varsayılan `en`) var, query ile dil seçimi yok.
2. **Locale fallback simetrik.** Spec "yoksa TR fallback"; kod seçilen dil boşsa DİĞER dile düşer (en boşsa tr, tr boşsa en). `nameTr/nameEn` zorunlu olduğu için `name` her zaman doludur; yalnız `description` etkilenebilir.
3. **Seed yalnız SQL'de.** 9 seed portal (`genel-sohbet`, `westeros`, `stranger-things`, `severance`, `from`, `pluribus`, `breaking-bad`, `it-welcome-to-derry`, `the-bear`) `portal_migration.sql` içinde; Java tarafında seed runner/config YOK. Migration çalıştırılmayan (boş `ddl-auto=update`) bir DB'de portal yoktur ve `POST /threads` her `portalSlug` için 404 verir, admin önce portal oluşturmalı. İsim/açıklama/sort_order metinleri placeholder (spec: "geçici varsayılanlar"); seed'deki yapım slug'ı DB'de yoksa o bağ atlanır (migration sonunda rapor sorgusu listeler). Backfill kuralı (varsayım): `thread.production_slug` bir portala bağlıysa o portal, değilse `genel-sohbet`.
4. **Thread.portalId düz `Long`** (spec: gerçek `@ManyToOne` veya düz Long; ikincisi seçildi). `fk_thread_portal` FK'sı ve NOT NULL yalnız `portal_migration.sql` ile oluşur; Hibernate `ddl-auto=update` FK açmaz (entity kolonu `nullable=false`).
5. **İndeksler 3 değil 4** (`(portal_id,status,created_at)`, `(portal_id,status,surface,created_at)`, `(portal_id,status,hot_score)`, `(portal_id,status,surface,hot_score)`) + comment tablosunda `idx_comment_status_created_subject`. Spec'teki `(portalId, surface, createdAt)` `status`'suz olduğu için kullanılmadı (gerekçe migration dosyasında). Bu FE'yi etkilemez.
6. **Portal boyut/sayfa sınırı:** dizinde `size` 1..50'ye sessizce kırpılır (spec yalnız "size(20)" diyordu). `sort` bilinmeyen değerde hata yerine `trending`.
7. **Trending formülü (spec "tasarla"):** `3*yeniThread(7g) + yeniYorum(7g)`, eşitlikte `sortOrder`, sonra `id`. Cache/job yok; tüm görünür portal kümesi çekilir, bellekte sıralanıp dilimlenir (portal sayısı küçük varsayımı). `/me/portals?sort=activity` aynı formülü kullanır. Portal endpoint'lerinde Redis cache YOK (kullanıcıya özel `isMember` + dil nedeniyle bilerek).
8. **Ek uçlar/parametreler (spec'te olmayan, kodda var):** `GET /api/community/feed/cursor`'da `portal`, `productionSlug`, `tags`, `scope`; `GET /api/community/threads?portal=`; `GET .../threads/{id}/comments/cursor`; `DELETE /threads/{id}`; `DELETE`/`PATCH` slug varyantları. `/portals/{slug}/feed` ise cursor/`scope`/`productionSlug`/`tags` SUNMAZ.
9. **Taşımada `productionSlug` tutarlılığı yok.** Spec "productionSlug verildiyse seçili portalın yapımlarından biri olmalı" kuralını oluşturma için tanımlıyor; kod bunu yalnız `POST`'ta uyguluyor. `PATCH` ile taşınan thread eski `productionSlug`'ını korur (yeni portalın yapımı olmayabilir); PATCH'te `productionSlug` alanı yok. Admin bir portalın `productionSlugs`'ından bir yapımı çıkarırsa mevcut thread'ler etkilenmez.
10. **Moderatörün ARCHIVED hedefi kontrolü eşitlikten önce.** Moderatör thread'i zaten ARCHIVED olan MEVCUT portalın slug'ıyla PATCH'lerse (taşıma niyeti olmasa da) 400 alır; `resolveMoveTargetId` ARCHIVED kontrolünü "hedef = mevcut" no-op kontrolünden önce yapıyor. Aynı şekilde mevcut portal HIDDEN ise 404.
11. **Sahibin taşıma denemesi her zaman 403.** Hedef portal var mı/yok mu bakılmadan, mevcut slug'dan farklı her `portalSlug` (yok olan, HIDDEN olan dahil) 403 döner; 404/400 yalnız moderatör/admin için. `portalSlug: ""` gönderen sahip 403, moderatör 404 alır.
12. **PATCH/DELETE `{id}` görünürlüğü.** Thread DELETED/HIDDEN status'lu olabilir (düz `findById`, eski davranış), ama portalı `HIDDEN` ise sahip PATCH/DELETE edemez (404); moderatör/admin serbest. Portalı `ARCHIVED` ise sahip PATCH edemez (400), moderatör edebilir; DELETE herkes için açık. Yorum silme ve yorum like'ı da HIDDEN portalda (moderatör hariç) 404 verir.
13. **Yorum listesi tutarsızlığı.** `GET /threads/{id|slug}/comments[/cursor]` DELETED/HIDDEN thread için 404 döner; merkezi `GET /api/community/comments?subjectType=THREAD&subjectId=` ise DELETED thread'in yorumlarını okuyabilir (yalnız HIDDEN-PORTAL thread'i 404). Mevcut davranış korundu.
14. **Join'de ARCHIVED + mevcut üye = 200**, HIDDEN portaldan `leave` = 404 (üye HIDDEN portaldan ayrılamaz, `memberCount`'a dahil kalır). Spec bu iki köşeyi tanımlamıyordu. `join/leave` portal satırını `SELECT ... FOR UPDATE` ile kilitler (spec'in "INSERT IGNORE / native UPDATE" isteği + kilit eklendi; yanıttaki `memberCount` kilit altında okunan değerin ±1'idir).
15. **Görsel doğrulaması.** Spec "folder `portals`" diyordu; kod `MediaUrlValidator.isOwnedImage` ile YALNIZ projenin Cloudinary'sine ait görsel olduğunu doğruluyor, `fandoom/portals/` klasörü zorunlu değil. Portal görseli değiştirilince/kaldırılınca eski Cloudinary dosyası SİLİNMEZ (diğer modüllerdeki `deleteIfChanged` deseni uygulanmadı; spec "yalnız URL saklar"). Admin PATCH'te `nameTr/nameEn` yalnızca boşluk içeren değer (`"   "`) reddedilir (400), create'teki `@NotBlank` ile tutarlı.
16. **Genel hata işleyicisi (çözüldü).** Bozuk JSON / bilinmeyen enum / okunamayan gövde -> 400; yanlış HTTP metodu -> 405 (`Allow` başlığı); desteklenmeyen içerik tipi -> 415; bulunamayan yol -> 404. Artık 500'e düşmez. Gövde her zaman `ApiErrorResponse`.
    - **Var olmayan thread/yapım detayı (çözüldü):** Redis erişilemezken `GET /threads/{id|slug}` (ve diğer `@Cacheable(sync=true)` detay okumaları) yok olan kayıt için 404 yerine 500 dönüyordu (var olanlar 200). `FailSafeCache` ile düzeltildi; Redis kapalıyken de 404.
17. **`portal` teorik olarak null olabilir.** Kod, portal satırı çözülemezse `portal: null` döner (portalId null veya silinmiş portal). `thread.portal_id` NOT NULL ve migration FK'sı ile bu durum oluşmaz; yalnız migration çalıştırılmamış bir DB'de (FK yok) teorik.
18. **Slug büyük/küçük harf.** Portal slug'ı DB araması (`findBySlug`) collation'a bağlı; MySQL varsayılan collation'ı büyük/küçük harf duyarsız olduğundan `/portals/Westeros` çözülebilir (varsayım — collation doğrulanmadı). Thread cache anahtarı portal slug'ını küçük harfe normalize eder; `PATCH` sahip kontrolü `equalsIgnoreCase` kullanır. FE her zaman küçük harf kanonik slug kullanmalı.
19. **`scope=joined` + `portal`.** Kullanıcı üye olmadığı bir portalı `portal=` ile verirse 404 değil BOŞ sayfa döner (portal yok/HIDDEN ise 404). Üye olunan ARCHIVED portalların thread'leri joined akışta görünür.
20. **Cache (thread).** Anonim + ilk 5 sayfa liste (yalnız `productionSlug` verilMEdiğinde; rastgele slug ile sınırsız key üretilmesin diye) ve anonim thread detayı Redis'te 1 saat TTL cache'lenir; tüm thread/yorum/like/bookmark/portal-admin yazmaları `allEntries` evict eder. Portal üyelik (join/leave) thread cache'ini temizlemez (thread yanıtlarında üyelik bilgisi yok). Dönen sayaçlar (`likeCount` vb.) anonim yanıtlarda kısa süreli bayat olabilir; `hotScore` 15 dakikalık job ile güncellenir.
21. **Doğrulanmamış varsayımlar.** (a) FE rotası `/portal/{portalSlug}/post/{id}-{post-slug}` spec'ten alındı, backend'de buna bağlı bir kod yok. (b) Hata `message` metinleri Türkçe sabit, `fieldErrors` mesajı Bean Validation'ın dil-bağımlı varsayılan metnidir; örneklerdeki İngilizce metin tipik değerdir. (c) `tags`/`media`/`author` alanlarının ayrıntıları bu sözleşmede yalnız portal etkisi kadar anlatıldı; onlar mevcut thread sözleşmesinden değişmedi.
