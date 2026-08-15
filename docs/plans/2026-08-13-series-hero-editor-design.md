# Series Hero Editor — Tasarım

**Tarih:** 2026-08-13
**Durum:** Onaylandı, implementasyon bekliyor.

## Amaç

Admin panelinde, Series detay sayfasının en üstündeki "Hero" bileşenini
(arka plan görseli + blur, logo, başlık, meta bilgi, sinopsis, fragman
butonu, serbest metin kutuları) admin'in serbestçe konumlayıp
ekleyip/çıkarabildiği bir editör. Blog'un blok tabanlı gövde editörüyle
(`blog/BlogBlock`) aynı ruhta ama Series'e özel yeni stil alanları
(blur, font, radius) taşır.

## Mevcut durumla ilişki

- `cms/HomeBlock`'un `PageName.SERIES_DETAIL` + sabit `SectionName` deseni
  bu iş için **kullanılmıyor** — Hero'nun alanları (blur, font, radius)
  sayıca çok ve tipli, HomeBlock'un "her alan ayrı satır, opak
  `contentValue` string" deseni tip güvenliğini kaybettirir.
- `blog/BlogBlock` emsal al��ndı: `ContentBlock`'u extend eder ama
  `col/row`'u kullanmaz, kendi `x/y/width/height` (serbest kanvas
  konumlama) alanlarını tanımlar. `SeriesHeroBlock` aynı deseni izler.
- `Series` entity'sinde **zaten** `trailerUrl`, `coverImageUrl`,
  `imdbId`, `externalRating`, `externalVoteCount`,
  `externalRatingUpdatedAt` var (admin `SeriesRequest` üzerinden elle
  giriyor). Hero bu alanları **tekrar saklamaz**, render anında
  Series'ten okur.

## Entity: `series/entity/SeriesHeroBlock`

`ContentBlock`'u extend eder (id/orderIndex miras; col/row
kullanılmaz). `Series`'e gerçek `@ManyToOne` FK
(`fk_series_hero_block_series`). `Series` entity'sine
`@OneToMany heroBlocks` (cascade ALL + orphanRemoval, `@OrderBy
orderIndex ASC`) + `addHeroBlock`/`clearHeroBlocks` helper'ları eklenir
— `Blog.blocks`/`addBlock`/`clearBlocks` ile birebir aynı kalıp. Ayrı
bir "SeriesHero" sarmalayıcı entity yok; `Series`'in kendisi zaten
sarmalayıcı. "Ekle/çıkar" = bu listede CRUD (bulk replace, aşağıya
bkz.).

### Alanlar

| Alan | Tip | Hangi blockType'ta anlamlı |
|---|---|---|
| `blockType` | enum `SeriesHeroBlockType` | hepsi |
| `x, y, width, height` | Double | hepsi (BlogBlock ile aynı) |
| `imageUrl` | String(500) | IMAGE, LOGO |
| `blurAmount` | Integer (px) | yalnızca IMAGE |
| `textTr, textEn` | String | yalnızca BOX |
| `backgroundColor` | String (hex) | yalnızca BOX |
| `borderRadius` | enum `RadiusToken` | IMAGE, BUTTON, BOX |
| `fontFamily` | enum `HeroFontFamily` | TITLE, SYNOPSIS, META, BOX |
| `fontScale` | Double (default 1.0) | TITLE, SYNOPSIS, META, BOX |

Tipe göre kullanılmayan alanlar null kalır — `HomeBlock`/`BlogBlock`'ta
zaten var olan "tipe göre koşullu alan" konvansiyonu.

### `SeriesHeroBlockType` enum

```
IMAGE   — arka plan görseli, blurAmount ile blurlanabilir
LOGO    — dizi logosu/wordmark görseli
TITLE   — Series.titleTr/titleEn'i otomatik yansıtır, kendi metni YOK
META    — Series verisinden (yıl, sezon sayısı, content rating vb.)
          FE'de kompoze edilen satır; backend'de metin alanı YOK
SYNOPSIS— Series.synopsisTr/synopsisEn'i otomatik yansıtır, kendi metni YOK
BUTTON  — hedefi her zaman Series.trailerUrl; linkUrl alanı YOK.
          Stili her zaman glassmorfik (buttonStyle alanı YOK, backend'de
          hiç tutulmuyor) — FE'nin mevcut .hero__trailer CSS'i zaten
          backdrop-filter: blur(20px) saturate(200%) brightness(1.15)
          kullanıyor; bu sabit değer blurAmount alanıyla KARIŞTIRILMAZ,
          blurAmount yalnızca IMAGE bloğunun arka plan görseline uygulanır.
BOX     — admin'in serbestçe textTr/textEn yazdığı, backgroundColor
          (hex) ve borderRadius ayarlanabilen kutu
```

TITLE/SYNOPSIS/META/BUTTON blokları render anında Series'ten otomatik
beslenir: ilgili Series alanı boşsa (ör. `trailerUrl` null) FE o bloğu/
rozeti otomatik gizler, blok DB'de konumlanmış halde dursa bile. Admin
ayrıca bir "göster/gizle" toggle'ı yönetmez.

### `RadiusToken` enum

`SM, MD, LG, PILL` — FE'nin mevcut `--radius-*` CSS token adlarıyla
birebir eşleşir (keyfi px değeri değil).

### `HeroFontFamily` enum

Blog'un `BlockFontFamily`'sinden **bağımsız**, yeni enum (series/
modülünün blog/'a bağımlı olmaması için). Başlangıç değeri:

```
COOPER_BT
```

(İleride yeni font gerektiğinde enum'a eklenir.)

## Endpoint'ler (`series/controller/`)

`series/` modülünün kendi konvansiyonuna sadık kalındı: `Season`/
`Episode` gibi aggregate çocuklar `SeriesRequest`'e gömülü değil, kendi
uçlarına sahip (Blog'un `blocks`'u `BlogRequest`'e gömme deseninden
kasıtlı farklı).

- `GET /api/series/{id}/hero-blocks` — public, `List<SeriesHeroBlockResponse>`.
- `PUT /api/series/{id}/hero-blocks` — `List<@Valid SeriesHeroBlockRequest>`
  body, tüm listeyi tek seferde değiştirir (editör "kaydet" tüm canvas
  state'ini gönderir) — `BlogServiceImpl.applyBlocks` ile aynı
  bulk-replace mantığı (`clearHeroBlocks()` + yeniden ekle + `orderIndex`
  ata). Rol: `EDITOR/MODERATOR/ADMIN`.

Yeni dosyalar: `series/entity/SeriesHeroBlock`,
`series/entity/SeriesHeroBlockType`, `series/entity/RadiusToken`,
`series/entity/HeroFontFamily`, `series/dto/SeriesHeroBlockRequest`,
`series/dto/SeriesHeroBlockResponse`, `series/mapper/SeriesHeroBlockMapper`
(gerekirse), servis metodu `SeriesService`/`SeriesServiceImpl`'e eklenir
(ayrı bir `SeriesHeroService` açmaya gerek yok, Blog'da da ayrı bir
`BlogBlockService` yok).

**Güvenlik notu (implementasyonda doğrulandı):** `SecurityConfig`'te
`/api/series/**` zaten hem GET-permitAll hem de
EDITOR/MODERATOR/ADMIN yazma kuralına dahil (wildcard) — yeni
`hero-blocks` alt-yolu için ayrı bir kural eklemeye gerek kalmadı.

## Kapsam dışı bırakılanlar (bilinçli, sonraya bırakıldı)

- **OMDb canlı rating çekme.** `GET /api/series/{id}/imdb-rating` gibi
  bir endpoint (OMDb'ye istek atıp `Series.externalRating`/
  `externalVoteCount`/`externalRatingUpdatedAt`'i cache olarak
  kullanarak güncelleyen) şimdilik yazılmıyor. Hero, mevcut
  admin-elle-girilen `Series.externalRating`/`imdbId`'yi olduğu gibi
  okuyacak. İleride eklenirse: ayrı cache katmanı/Redis gerekmez,
  mevcut `externalRatingUpdatedAt` kolonu zaten "ne zaman tazelendi"
  bilgisini taşıyor.
- **Movie için eşdeğer Hero editörü** — bu görev yalnızca Series'i
  kapsıyor, Movie'ye genişletme ayrı bir görev.
- **Buton stili seçimi (SOLID/OUTLINE vb.)** — Hero butonları her zaman
  glassmorfik, backend'de `buttonStyle` alanı yok.

## Açık uçlar / gelecekte netleşecek

- `HeroFontFamily` enum'u ileride yeni fontlarla genişleyecek.
- BOX bloğunun textTr/textEn için maksimum uzunluk sınırı implementasyon
  aşamasında (`@Size`) belirlenecek.
