# Arama — API Sözleşmesi (FE referansı)

**Durum: TASLAK (2026-10-15).** Backend kodu henüz YAZILMADI. Bu sözleşme, frontend'in backend'le eş zamanlı başlayabilmesi için tasarım belgesinden (`docs/plans/2026-10-15-unified-search-design.md`) türetildi. Kod yazıldıktan sonra bu metin koda göre güncellenir; çelişki olursa KOD esastır.

İki seviye arama:
- **Quick search:** header'daki arama kutusu, yazarken açılan liste. Tüm türler tek yanıtta.
- **Full search:** Enter'a basınca açılan arama sayfası. Sekme başına tek tür, sayfalı.

---

## 0. Genel kurallar

- Base path: `/api/search`. **Tüm uçlar GET ve public** (anonim çağrılır, token gerekmez).
- **Dil:** `Accept-Language` başlığı (`tr` | `en`, yoksa/başkasıysa `en`). Gösterilen başlık/isim seçilen dile göre çözülür, seçilen dilin alanı boşsa diğer dile düşer. **Eşleştirme dilden bağımsızdır:** Türkçe kullanıcı İngilizce başlık yazarsa da bulur.
- **`q` kuralları (tüm uçlarda aynı):**
  - Baştaki ve sondaki boşluklar kırpılır.
  - Büyük/küçük harf duyarsızdır.
  - Kırpılmış hâli **2 karakterden kısaysa** hata dönmez, boş sonuç döner (`200`).
  - **100 karakterden uzunsa → `400`.**
  - `%` ve `_` normal karakter olarak aranır (joker değildir).
- **Eşleşme:** "İçinde geçen". `q=part` → "Dune: Part Two" bulunur. Yalnızca başlık/isim alanları taranır; açıklama, sinopsis ve gövde metni taranmaz.
- **Sıralama:** Önce `q` ile **başlayanlar**, sonra içinde geçenler; her grup kendi içinde alfabetik. Örnek: `Dune` → `Dune: Part Two` → `Children of Dune`. `sort` parametresi kabul edilmez/yok sayılır.
- **Kapsam (4 tür):** Film, dizi, portal, blog. Kişi (Person) ve karakter (Character) aramaları **şimdilik kapsam dışı** (2026-10-15 kararı). İleride eklenirse yeni bir grup tipi ve `/api/search/{people,characters}` uçları gelir, mevcut şekiller değişmez.
- **Görünürlük:** Bloglarda yalnızca yayınlanmış (`PUBLISHED`) olanlar, portallarda `ACTIVE` + `ARCHIVED` olanlar (HIDDEN asla) görünür. Film ve dizilerin hepsi görünür.
- **Önerilen FE davranışı:** Quick search için **debounce (~300ms)**: kullanıcı yazmayı bırakınca istek at, her tuşta atma.
- Hata gövdesi (tüm modüllerle ortak):

```json
{
  "timestamp": "2026-10-15T16:40:02.118",
  "status": 400,
  "error": "Bad Request",
  "message": "q en fazla 100 karakter olabilir",
  "path": "/api/search/quick",
  "fieldErrors": null
}
```

---

## 1. Quick search

```
GET /api/search/quick?q=dune
```

| Parametre | Zorunlu | Açıklama |
|---|---|---|
| `q` | evet | Arama metni (bkz. §0 kuralları) |

- Her türden **en fazla 5** sonuç döner (sabit, parametre yok).
- Tür sırası sabittir: `MOVIE`, `SERIES`, `PORTAL`, `BLOG`.
- **Sonucu olmayan türün grubu yanıtta hiç yer almaz.** `q` 2 karakterden kısaysa `groups: []` döner.
- Yoğun anlarda bir tür zamanında yanıt veremezse o tür **sessizce boş** döner (grup yer almaz). Yanıtta bunu belirten bir alan **yoktur**; FE için normal "sonuç yok" durumundan farkı yoktur.
- Sonuçlar 1 saate kadar önbellekten gelebilir: yeni eklenen içerik aramada gecikmeli görünebilir.

### Yanıt

```json
{
  "groups": [
    {
      "type": "MOVIE",
      "items": [
        { "id": 3,  "slug": "dune",          "title": "Dune",          "subtitle": "2021", "imageUrl": "https://res.cloudinary.com/..." },
        { "id": 17, "slug": "dune-part-two", "title": "Dune: Part Two", "subtitle": "2024", "imageUrl": null }
      ]
    },
    {
      "type": "BLOG",
      "items": [
        { "id": 8, "slug": "dune-col-ekolojisi", "title": "Dune'un çöl ekolojisi", "subtitle": "İnceleme", "imageUrl": "https://res.cloudinary.com/..." }
      ]
    }
  ]
}
```

### `SearchHit` (her `items` elemanı)

| Alan | Tip | Açıklama |
|---|---|---|
| `id` | number | Kaydın id'si |
| `slug` | string | Detay sayfası linki için (`type` ile birlikte route kurulur) |
| `title` | string | Gösterilecek ad, dile göre çözülmüş |
| `subtitle` | string \| null | Alt satır (aşağıdaki tablo) |
| `imageUrl` | string \| null | Küçük görsel (Cloudinary URL'i; boyutlandırma FE'de) |

| `type` | `title` kaynağı | `subtitle` | `imageUrl` kaynağı |
|---|---|---|---|
| `MOVIE` | film adı | çıkış yılı (`"2021"`), tarih yoksa `null` | afiş |
| `SERIES` | dizi adı | ilk yayın yılı, yoksa `null` | afiş |
| `PORTAL` | portal adı | `null` | portal ikonu |
| `BLOG` | blog başlığı | kicker (üst başlık), yoksa `null` | kapak görseli |

---

## 2. Full search

Tür başına ayrı uç. Hepsi aynı parametreleri alır ve aynı sayfa zarfını döndürür; yalnızca `content` elemanlarının şekli türe göre değişir.

```
GET /api/search/movies?q=dune&page=0&size=20
GET /api/search/series?q=...
GET /api/search/portals?q=...
GET /api/search/blogs?q=...
```

| Parametre | Zorunlu | Varsayılan | Açıklama |
|---|---|---|---|
| `q` | evet | — | Arama metni (bkz. §0) |
| `page` | hayır | `0` | 0 tabanlı sayfa numarası |
| `size` | hayır | `20` | En fazla `50` |

Full search önbelleklenmez; yeni içerik hemen görünür.

### Sayfa zarfı (`PageResponse`)

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 47,
  "totalPages": 3,
  "last": false
}
```

Numaralı sayfalama (`1 2 3 ›`) için `totalElements` / `totalPages` kullanılabilir.

### `content` elemanları (mevcut liste uçlarıyla aynı şekiller)

**movies** (`MovieSummaryResponse`)
```json
{ "id": 3, "title": "Dune", "titleTr": "Dune: Çöl Gezegeni", "slug": "dune", "posterUrl": "...",
  "releaseDate": "2021-10-22", "externalRating": 8.0, "status": "RELEASED" }
```
`title` dile göre çözülmüş, `titleTr` ham. `status`: `ANNOUNCED | FILMING | POST_PRODUCTION | RELEASED | CANCELLED` (null olabilir).

**series** (`SeriesSummaryResponse`)
```json
{ "id": 5, "title": "Dune: Prophecy", "titleTr": null, "slug": "dune-prophecy", "posterUrl": "...",
  "status": "...", "firstAirDate": "2024-11-17", "externalRating": 7.4 }
```

**portals** (`PortalSummaryResponse`)
```json
{ "id": 2, "slug": "dune", "name": "Dune", "description": "...", "bannerUrl": "...", "iconUrl": "...",
  "accentColor": "#C2763A", "memberCount": 120, "threadCount": 45, "isMember": false,
  "postingPolicy": "OPEN", "status": "ACTIVE", "productionSlugs": ["dune", "dune-part-two"] }
```
`status` `ACTIVE` veya `ARCHIVED` olabilir (arşivli portal salt-okunur, FE rozet gösterebilir). `isMember` token varsa gerçek değer, anonimde `false`.

**blogs** (`BlogSummaryResponse`)
```json
{ "id": 8, "slug": "dune-col-ekolojisi", "title": "Dune'un çöl ekolojisi", "imageUrl": "...", "imageAlt": "...",
  "readingTimeMinutes": 6 }
```

---

## 3. Eski uçlardan geçiş

- `GET /api/movies?search=...` ve `GET /api/series?search=...` **kaldırılacak**. Admin paneli (`admin/entities/entities.api.js` → `listEntities`'in `search` parametresi) `/api/search/{tür}` uçlarına taşınmalı.
- **Sıra:** backend yeni uçları açar → FE admin geçer → backend eski dalı siler.
- Yeni uçlar yalnızca yayındaki içeriği gösterir: admin aramasında taslak bloglar ve HIDDEN portallar **çıkmaz** (bilinçli kabul).
- Kişi ve karakter araması admin'de şu an hiç çalışmıyor (backend `search` parametresini tanımıyordu). Bu türler aramaya şimdilik dahil değil (bkz. §0 Kapsam), admin'de de aranamamaya devam eder.
