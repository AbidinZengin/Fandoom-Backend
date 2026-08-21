# Season Blocks (`season.seasonBlocks[]`) — Tasarım

**Tarih:** 2026-08-21
**Durum:** Onaylandı, implementasyon bekliyor.
**Kaynak:** Frontend'den gelen "Backend İhtiyacı: `season.seasonBlocks[]`"
taslak notu — `episode.episodeBlocks[]` deseninin sezon seviyesine
taşınması isteği (Breaking Bad 1-5 arası "derin okuma" sayfaları, şu an
frontend'e gömülü mock veri).

## Amaç

`episode.episodeBlocks[]` (bölüm-seviyesi editoryel derin analiz) zaten
üretimde. Aynı içerik türü **sezon seviyesinde** de var ama backend'de
karşılığı yok. `seasonBlocks[]`, `episodeBlocks[]`'ın Season'a taşınmış
hali — ayrı bir alan (aynı array'in overload edilmesi değil), çünkü
`episodeBlocks` adı Episode entity'sine özel kalmalı.

## Mevcut durumla ilişki — kaynak nottan sapmalar

Nottaki taslak, üç noktada `episode/EpisodeBlock` mevcut deseninden
sapıyordu; bu tasarım dokümanı bunları düzeltiyor:

1. **TR/EN alan sayısı.** Not, `storyKicker/storyKickerTr/storyKickerEn`
   gibi 3 alanlı bir şema öneriyordu ("Episode'da `*En` eksikti, bu bir
   eksiklikti" diyerek). Gerçekte `EpisodeMapper`
   (`series/mapper/EpisodeMapper.java`) her alan için
   `LocalizedTextResolver.resolve(trValue, enValue)` kullanıyor — DB'de
   **iki** kolon var (`x`, `x_tr`), `x` zaten "EN/varsayılan" değeri
   taşıyor; ayrı bir `xEn` hiç yok. Bu tasarım 2 alanlı konvansiyona
   sadık kalır.
2. **`mediaEpisodeRef`'in tipi.** Not bunu "yeni alan tipi" olarak
   sunuyordu ama gerçekte `Season`/`Episode` aynı aggregate içinde
   (`series/` modülü) — CLAUDE.md'nin "aggregate içi ilişkiler gerçek
   JPA ilişkisi kurabilir" istisnası burada geçerli olabilirdi. Ama
   notun kendi tarif ettiği kullanım ("frontend bu numarayla
   `episodes[]` listesinden `stillImageUrl`'i eşleştiriyor") bir
   sunum-zamanı eşleştirme anahtarı, referential-integrity gerektiren
   bir ilişki değil — `EpisodeBlock.sceneKey`'in "opak anahtar, DB'de
   gerçek ilişki yok" felsefesiyle aynı. Gerçek FK yerine düz
   `Integer` kolon (episode numarası) tercih edildi.
3. **Medya alanlarının blockType'a bağlılığı.** Not, medya alanlarını
   (`mediaEpisodeRef`/`mediaCaption`/`mediaCredit`) her `blockType`'a
   eklenti gibi listeliyordu, ayrı bir `MEDIA` tipi önermiyordu.
   `EpisodeBlock`'ta medya kendi başına bir `blockType` (satırın tamamı
   görsel, `content` null kalır). Tutarlılık için `SeasonBlockType`'a
   `MEDIA` eklendi; medya alanları yalnızca `MEDIA` tipinde dolu olur.

## Entity: `Season` — yeni skaler alanlar

`Episode`'daki `storyKicker/storyTitle/storyThesis` karşılığı, 2 alanlı
TR/EN deseniyle (bkz. yukarı):

| Alan | Not |
|---|---|
| `storyKicker` / `storyKickerTr` | Kısa etiket — "DEEP READING · SEASON 1" |
| `storyTitle` / `storyTitleTr` | Başlık |
| `storyDek` / `storyDekTr` | Tek cümlelik alt başlık (Episode'daki `storyThesis`'in sezon karşılığı — bilinçli farklı isim, farklı ton: "dek" gazetecilikte alt başlık demek, "thesis" Episode'un kendi tercihiydi) |

Episode'daki gibi tüm sezonlarda dolu olmak zorunda değil, yazılmamış
sezonlarda null.

## Entity: `series/entity/SeasonBlock`

`ContentBlock`'u extend eder (`EpisodeBlock` ile birebir aynı iskelet —
id/orderIndex miras, col/row kullanılmaz). `Season`'a gerçek
`@ManyToOne` FK (`fk_season_block_season`). `Season` entity'sine
`@OneToMany seasonBlocks` (cascade ALL + orphanRemoval, `@OrderBy
orderIndex ASC`) + `addSeasonBlock`/`clearSeasonBlocks` helper'ları
eklenir — `Episode.episodeBlocks`/`addEpisodeBlock`/`clearEpisodeBlocks`
ile birebir aynı kalıp.

### Alanlar

| Alan | Tip | Not |
|---|---|---|
| `blockType` | enum `SeasonBlockType` | aşağı bkz. |
| `sceneKey` | String(100) | Aynı section'a ait bloklar aynı key'i paylaşır (Episode ile aynı mantık) |
| `orderIndex` | int | Miras (ContentBlock) |
| `content` / `contentTr` | TEXT | MEDIA'da null |
| `mediaEpisodeRef` | Integer, nullable | Yalnızca MEDIA'da dolu — FK DEĞİL, `Episode.episodeNumber`'a opak referans |
| `mediaCaption` / `mediaCaptionTr` | String(255) | Yalnızca MEDIA'da anlamlı |
| `mediaCredit` | String(255) | Yalnızca MEDIA'da anlamlı, sabit değer (ör. "AMC") — TR/EN ayrımı yok, kaynak ismi zaten dil-bağımsız |

`EpisodeBlock`'taki `tone`/`pinned`/`lead` alanları burada YOK — nottaki
6 blockType değeri (`LEDE_TEXT`/`SECTION_HEADING`/`SECTION_TEXT`/
`SECTION_LEAD_TEXT`/`QUOTE`/`VERDICT_TEXT`) zaten "lead" ayrımını ayrı
bir enum değeriyle (`SECTION_LEAD_TEXT`) taşıyor — Episode'daki gibi
ayrı bir boolean `lead` flag'ine gerek yok.

### `SeasonBlockType` enum

```
LEDE_TEXT          — sezonun açılış paragrafı
SECTION_HEADING     — bölüm/section başlığı
SECTION_TEXT        — normal gövde metni
SECTION_LEAD_TEXT    — vurgulu/büyük punto açılış paragrafı (section içi)
MEDIA               — bölüm fotoğrafı yeniden kullanımı (mediaEpisodeRef ile)
QUOTE                — alıntı
VERDICT_TEXT         — kapanış/değerlendirme metni
```

`EpisodeBlockType`'tan bağımsız, ayrı enum — Interface Segregation
ilkesiyle tutarlı (her block ailesi kendi tipini taşır, paylaşılmaz).

## Servis katmanı

`SeasonServiceImpl.applySeasonBlocks`, `EpisodeServiceImpl.applyEpisodeBlocks`
(`series/service/EpisodeServiceImpl.java:113-140`) ile birebir aynı
kural: `requests == null` ise mevcut bloklara **dokunulmaz**, yalnızca
açıkça boş liste (`[]`) gönderilirse bloklar temizlenir. Bu kural, geçen
commit'te (`9b5c3a0`, "9 entity icin generic PUT/POST DTO genisletmesi")
kritik bug fix olarak eklendi — generic scalar-only PUT çağrısında
blokların yanlışlıkla silinmesini önlüyor. PageBuilder'ın Season
editörü de bu ayrıma uymalı: skaler-only kaydetmede `seasonBlocks`
alanını hiç göndermemeli, temizlemek istediğinde açıkça `[]` göndermeli.

`mediaEpisodeRef` için servis katmanında ekstra bir doğrulama/FK
kontrolü YAPILMAZ (bilinçli) — opak referans, geçersiz/olmayan bir
episode numarası verilse bile create/update patlamaz, sadece frontend
eşleşmeyi bulamayıp sessizce göstermez.

## DTO'lar

- `SeasonBlockRequest` (record) — `EpisodeBlockRequest` ile aynı şekilde,
  `@NotNull blockType`, `@NotBlank @Size(max=100) sceneKey`,
  `contentTr`/`content`, `mediaEpisodeRef` (Integer), `mediaCaptionTr`/
  `mediaCaption`, `@Size(max=255) mediaCredit`. **`col`/`row` bilinçli
  olarak DTO'ya taşınmadı** — `EpisodeBlockRequest`'in aksine (o zaten
  yayında, geriye dönük kırmamak için dokunulmadı): Season blokları hep
  sıralı/anlatısal (orderIndex + sceneKey grubu), asla CSS grid'de
  konumlanmıyor; `col`/`row` yalnızca `HomeBlock`'ta gerçek anlam
  taşıyor. Entity `ContentBlock`'tan miras aldığı için DB kolonları
  (`col`, `grid_row`) yine de var, sadece hep null kalacak — servis
  katmanı bunları hiç set etmiyor.
- `SeasonBlockResponse` (record) — `content`/`contentTr`,
  `mediaCaption`/`mediaCaptionTr` (resolve edilmiş + ham TR ikilisi,
  `EpisodeBlockResponse` ile aynı şekil).
- `SeasonRequest`'e `storyKickerTr/storyKicker`, `storyTitleTr/storyTitle`,
  `storyDekTr/storyDek`, `List<SeasonBlockRequest> seasonBlocks` eklenir.
- `SeasonDetailResponse`'a aynı skaler alanlar +
  `List<SeasonBlockResponse> seasonBlocks` eklenir. `SeasonSummaryResponse`
  değişmez (liste görünümünde blok gövdesine gerek yok — `Episode`
  emsalinde de `episodeBlocks` yalnızca `EpisodeResponse`'ta var, ayrı
  bir "özet" tipi zaten yok çünkü Episode'un kendi özeti yok).

## Mapper

`SeasonMapper`'a `EpisodeMapper.toBlockResponse` ile aynı şekilde:

```java
@Mapping(target = "content", expression = "java(LocalizedTextResolver.resolve(block.getContentTr(), block.getContent()))")
@Mapping(target = "mediaCaption", expression = "java(LocalizedTextResolver.resolve(block.getMediaCaptionTr(), block.getMediaCaption()))")
SeasonBlockResponse toBlockResponse(SeasonBlock block);
```

`toDetailResponse`'a `storyKicker`/`storyTitle`/`storyDek` için de aynı
`LocalizedTextResolver.resolve(...)` expression'ları eklenir.

## API sözleşmesi

Ayrı bir endpoint AÇILMAZ — `episodeBlocks[]` zaten `EpisodeRequest`/
`EpisodeResponse`'a gömülü (Series Hero'nun aksine, ayrı
`/hero-blocks` alt-yolu yok), `seasonBlocks[]` de aynı şekilde mevcut
`GET/PUT/POST /api/seasons/**` uçlarına gömülü kalır. `GET
/api/seasons/{id}` tek çağrıda `seasonBlocks[]`'ı inline döndürür (N+1
riski yok, `@OneToMany(fetch=LAZY)` + aynı transaction içinde mapper
erişimi zaten Episode'da bu şekilde çalışıyor).

Rol kuralı değişmez: `/api/seasons/**` zaten `SecurityConfig`'te
EDITOR/MODERATOR/ADMIN yazma + public GET kapsamında (wildcard),
`seasonBlocks` için ayrı bir path kuralı gerekmiyor.

## "Migrasyon" — netleştirme

Nottaki "5. Migrasyon" başlığı CLAUDE.md'deki `contentblock_migration.sql`
anlamındaki migration (var olan DB verisini yeni şemaya taşıma) değil —
`season_block` tamamen yeni bir tablo, mevcut içerik DB'de değil,
frontend'in `SeasonStory.data.js` mock dosyasında. Bu, şema açıldıktan
sonra PageBuilder üzerinden yapılacak bir **içerik girişi** işi (5
sezonun içeriği zaten yazılı/doğrulanmış — veri dönüşümü, yeniden yazım
değil). SQL migration script'i gerekmiyor; istenirse tek seferlik bir
seed/bulk-insert script'i ayrıca değerlendirilebilir ama bu backend
şema teslimatının kapsamı dışında.

## Kapsam dışı bırakılanlar

- `EpisodeBlock`'taki `tone`/`pinned`/`lead` — sezon içeriğinde
  karşılığı yok, eklenmedi.
- `mediaEpisodeRef` için FK/referential-integrity — bilinçli olarak
  opak bırakıldı (yukarı bkz.).
- Ayrı `/api/seasons/{id}/season-blocks` alt-yolu — Episode
  emsaliyle tutarlı, gömülü DTO tercih edildi.
