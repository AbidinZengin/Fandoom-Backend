# Birleşik Arama (search/) Tasarımı

**Tarih:** 2026-10-15
**Durum:** Kullanıcı ile adım adım tartışılıp doğrulandı. Kod yazılmadı; implementasyon adım adım, kullanıcı onayıyla yapılacak.
**Kapsam:** Movie, Series, Portal, Blog üzerinde başlık/isim araması. (Person ve Character başta kapsamdaydı, 2026-10-15'te şimdilik çıkarıldı. Aşağıdaki "6 tür" ifadeleri tasarım anındaki hâli; uygulamada 4 tür. Bkz. "Sonraya bırakılanlar".) Header'daki anlık açılır kutu (quick search) + Enter sonrası açılan sayfalı arama ekranı (full search).

## Context

Şu an yalnızca Movie ve Series'te kendi uçlarında `?search=` ile başlık araması var (`MovieService.search`, `SeriesService.search`), Portal dizininde `q` parametresi var. Kullanıcı tek bir arama kutusundan tüm içerik türlerini bulabilmeli. Harici arama motoru (Elasticsearch) ve ML şimdilik kapsam dışı. Mevcut MySQL + Redis ile, mevcut modüllere en az dokunarak çözülecek.

## Kararlar

### 1. Modül sınırı: servis interface'leri

`search/` paketi diğer modüllerin repository/entity'lerine **dokunmaz**, yalnızca servis interface'lerini çağırır.

- **Neden:** "Hangi film aramada görünür?" gibi kurallar ilgili modüle aittir. `search/` repository'ye doğrudan erişseydi bu kural iki yerde yaşardı ve modül kendi tablosunu değiştirdiğinde `search/` sessizce bozulurdu. `BlogSpecificationBuilder` istisnasının gerekçesi (Criteria API entity metamodeline ihtiyaç duyar) burada geçerli değil.
- **Bedel:** 6 ayrı sorgu. Tek bir SQL `UNION` ile türler arası global sıralama yapılamaz, her tür kendi içinde sıralanır. Quick search zaten kategorize gösterildiği için sorun değil (`production/` modülündeki bellek içi birleştirme ile aynı türden bilinçli trade-off).

### 2. Modüllerin döndürdüğü şey: her modül kendi DTO'su

Her modül quick search için **kendi küçük DTO'sunu** döndürür (ör. `MovieSearchResult`). `search/` bunları ortak `SearchHit`'e çevirir.

- **Neden:** Modüller arama ekranının şeklinden habersiz kalır. `SearchHit` `search/dto/` içinde durabilir, çünkü modüller onu tanımaz. Böylece döngüsel bağımlılık riski olmaz (`movie/ → search/ → movie/` oluşmaz).
- Modül DTO'ları **her iki dildeki ham alanları** taşır (`title` + `titleTr`). Dil seçimi `search/`'te yapılır (bkz. Karar 7).

### 3. İki seviye arama

| | Quick search | Full search |
|---|---|---|
| Ne zaman? | Yazarken açılan kutu | Enter'a basınca açılan sayfa |
| Kaç tür? | 6 tür birden | Seçili tek tür |
| Sonuç | Tür başına en fazla 5 (sabit), ortak `SearchHit` | Sayfalı, türe özgü zengin DTO |
| Uç | `GET /api/search/quick?q=` | `GET /api/search/{movies,series,characters,people,portals,blogs}?q=&page=&size=` |

**`SearchHit`:** `id, slug, title, subtitle, imageUrl`. Header kutusundaki "küçük görsel + başlık + gri alt satır" satırının ortak şekli.

| Tür | title ← | subtitle ← | imageUrl ← |
|---|---|---|---|
| Movie | title / titleTr | releaseDate yılı | posterUrl |
| Series | title / titleTr | firstAirDate yılı | posterUrl |
| Character | name | — | imageUrl |
| Person | name | — | photoUrl |
| Portal | nameTr / nameEn | — | iconUrl |
| Blog | title / titleTr | kicker / kickerTr | imageUrl |

**Quick search yanıtı:** `groups: [{ type, items: [SearchHit] }]`.

**Full search neden `search/` üzerinden tek "cephe":** Arama altyapısı bir gün değişirse (MySQL FULLTEXT, Elasticsearch) yalnızca `search/`'ün içi değişir, frontend aynı adresleri kullanmaya devam eder. Girdi kuralları, aramaya özel rate limit ve arama logu tek yerde durur.

**Neden `?type=` yerine tür başına ayrı uç (`/api/search/movies`):** Her uç tek ve belli bir tip döndürür. `?type=` ile tek metot `PageResponse<?>` döndürmek zorunda kalırdı ve frontend yanıtın şeklini tahmin etmek zorunda kalırdı. Bedeli: controller'da 6 kısa metot, yeni tür eklenince yeni uç.

### 4. Projeksiyon: yalnızca quick search'te

- **Quick search:** Entity yüklenmez. Repository yalnızca gereken kolonları seçen JPQL constructor projeksiyonu ile modül DTO'sunu döndürür. Quick search her tuşta çalışabildiği için küçük israf binlerce kez tekrarlanır.
- **Full search:** Mevcut `XSummaryResponse` DTO'ları kullanılır. Zengin kart daha çok alan ister ve Enter'a basıldığında, yani seyrek çalışır.
- **Genel kural:** Projeksiyon her yerde değil, sık çalışan ve az alan gösteren yerde değerlidir.

### 5. Sayfalama: full search'te `Page` (COUNT'lu)

Numaralı sayfa düğmeleri ("47 sonuç, 1 2 3") için `totalElements`/`totalPages` gerekir. Bu da ayrı bir `COUNT(*)` sorgusu demek. Mevcut ölçekte (binlerce kayıt) maliyet ihmal edilebilir.

- **Ne zaman yeniden düşünülür:** Arama yanıtları >200ms olursa ya da slow query log'da COUNT sorguları görünürse `Slice`'a (`size+1` satır, `hasNext`) geçilir. Bu API sözleşmesini değiştirir (`totalElements` kalkar), frontend'in de değişmesi gerekir.
- Quick search'te COUNT yok, repository `List` döndürür.

### 6. Eşleşme ve sıralama

- **"İçinde geçen":** `LIKE '%q%'`. Kullanıcı "Part Two" yazınca "Dune: Part Two" bulunur.
- **Taranan alanlar:** Yalnızca başlık ve isimler. Açıklama/sinopsis taranmaz (gürültü). Movie/Series: `title`, `titleTr`, `originalTitle`. Portal: `nameTr`, `nameEn`. Blog: `title`, `titleTr`. Person/Character: `name`.
- **Sıralama (quick ve full aynı):** Önce "ile başlayan", sonra "içinde geçen", grup içinde alfabetik. Örnek: `Dune` → `Dune: Part Two` → `Children of Dune`.
- **Bedel:** Baştaki `%` indeks kullanılmasını engeller, tablo taranır. "Başlayanlar önce" sıralaması da veritabanının ilk N eşleşmede durmasını engeller. Bugünkü ölçekte fark edilmez.
- **Görünürlük:** Yalnızca `BlogStatus.PUBLISHED` bloglar. Portallarda ACTIVE + ARCHIVED (portal dizinindeki `PUBLIC_STATUSES` ile aynı; HIDDEN asla). Arşivli portal salt-okunur ama içeriği okunabilir; dizinde görünüp aramada görünmemesi tutarsız olurdu.

**Büyüme merdiveni** (bir basamağa ancak öncekinin yetmediği ölçüldüğünde çıkılır):

| Basamak | Ne? | Ne zaman? |
|---|---|---|
| 1 (şimdi) | `LIKE` + Redis cache | Binlerce kayıt |
| 2 | MySQL `FULLTEXT` indeks | On binler, aramalar yavaşlarsa |
| 3 | Elasticsearch vb. | Yüz binler, ya da yazım hatası toleransı gibi yeni bir ihtiyaç |

Ölçüm hedefi: quick search **< 40ms** (k6 / curl ile).

### 7. Dil

Kullanıcının dili isteğin başında **bir kez** okunur. Paralel çalışan iş parçacıkları isteğin `LocaleContextHolder`'ını görmez. Görmezlerse sessizce varsayılan dile düşer: çökme olmaz, sadece Türkçe kullanıcıya İngilizce başlık gösterilir. Bu hataya "bağlamın kaybolması" denir.

- **Bulmak dilden bağımsız:** Her zaman tüm başlık alanlarında aranır. Türk kullanıcılar film adlarını çoğu zaman İngilizce yazar.
- **Göstermek dile bağlı:** `tr` ise `titleTr`, boşsa `title`'a düşülür (`LocalizedTextResolver` kuralı).
- **Uygulama:** Modül DTO'ları iki dili ham taşıdığı için dil seçimi `search/`'te, sonuçlar toplandıktan sonra isteğin kendi iş parçacığında yapılır. Böylece paralel iş parçacıklarına dil taşımaya gerek kalmaz.

### 8. Paralellik ve veritabanı bağlantıları

Quick search 6 sorguyu paralel çalıştırır (`CompletableFuture.allOf`). Gecikme, en yavaş tek sorgunun süresine iner.

- **Risk:** Her quick search aynı anda 6 DB bağlantısı tutar. HikariCP havuzu varsayılan olarak 10 bağlantılıdır ve tüm site aynı havuzu paylaşır. Yoğun anda arama, film detayı ve yorum gibi sitenin geri kalanını da bekletebilir (varsayılan bekleme 30 sn).
- **Önlem:**
  - **Cache** (Karar 9). Popüler aramalar hiç bağlantı kullanmaz.
  - **Kısa bekleme + kısmi sonuç.** Tüm türler için ortak bir son tarih var (`search.quick.timeout`). Başta 1 sn idi, 2026-10-16'da **300 ms**'ye indirildi. Süreyi geçen tür vazgeçilir ve o grup boş döner. Bu süre bir bekleme değil, bir tavandır: normal günde sorgular ~20–45 ms'de döner ve tavan devreye girmez. Çok kısa tutmanın riski "ölüm sarmalı"dır: normal günde bile eksik cevap çıkar, eksik cevap cache'lenmez, DB yükü artar, daha fazla zaman aşımı olur. Kural: tavan, normal en yavaş sürenin birkaç katı olmalı. Bilinen bedel: soğuk başlangıçta ilk istekler (~500 ms) eksik dönebilir. Yük altında (k6) ölçülüp yeniden ayarlanmalı. Arama, gerektiğinde fedakârlık yapılacak taraftır: 6 kategoriden biri eksik gelirse kimse fark etmez, film sayfası açılmazsa site bozuktur.
- **Ertelenen:** Hat kotası (bulkhead, aramanın aynı anda en fazla N bağlantı kullanması). Gerçekten sorun görülürse eklenecek. Havuzu büyütmek sorunu çözmez, sadece erteler.

### 9. Cache

- Redis, **yalnızca quick search**, TTL **1 saat** (projenin global TTL'i; ayrı TTL yok). Yeni içeriğin aramada 1 saate kadar geç görünmesi kabul edildi.
- **Anahtar:** `dil : küçük harfli ve trimli metin`. Kural: **cevabı değiştiren her şey anahtarda olmalı.**
- **Eksik cevap cache'lenmez.** Bir tür zaman aşımına uğradıysa cevap kullanıcıya eksik döner ama Redis'e yazılmaz. Yazılırsa 1 dakikalık bir arıza 1 saat boyunca herkese yanlış cevap olarak gösterilir.
  - "Bulunamadı" (boş ama doğru) cache'lenir. "Getirilemedi" (arıza) cache'lenmez. Kod bu ikisini ayırt etmeli.
  - **Uygulama notu:** Proje kuralı her `@Cacheable`'ın `sync = true` olmasını istiyor. `sync = true` ise `unless` ile birlikte kullanılamıyor. **Karar (uygulandı, seçenek C):** `@Cacheable` yerine `CacheManager.getCache(...).get(key, loader)`. Bu, `sync = true`'nun kullandığı yolun aynısı, yani stampede koruması korunuyor. Cevap eksikse loader `IncompleteResultException` fırlatır. Exception fırlayınca cache hiçbir şey yazmaz; servis exception'ı yakalar ve içindeki kısmi cevabı döner. Bedeli: exception'ı akış kontrolü için kullanmak alışılmadık bir yöntem, bu yüzden kodda yorumla açıklandı. Kuyrukta bekleyenler eksik cevabı paylaşmaz, her biri kendisi dener. Değerlendirilip elenen seçenekler: A) elle yönetim, stampede koruması yok (bu ölçekte yeterli olurdu); B) kendi "single-flight" kilidimizi yazmak (mevcut mekanizmanın kopyası olurdu).
- Full search cache'lenmez (sayfa × sorgu × tür kombinasyonu sınırsız).
- Cache'e her farklı arama metni ayrı bir not olarak girer ("d", "du", "dun", yazım hataları). Bunu sınırlayan önlemler: TTL, küçük harf normalizasyonu, en az 2 harf şartı ve frontend debounce.

### 10. Girdi validasyonu

| Kural | Karar |
|---|---|
| Trim | Hem sorgu hem cache anahtarı |
| En az 2 karakter | Altındaysa boş sonuç döner, DB'ye ve cache'e gidilmez |
| En fazla 100 karakter | Aşılırsa 400 (Portal aramasıyla tutarlı) |
| `%` ve `_` | LIKE jokerleri kaçışlanır (`ESCAPE '!'`). Ortak araç `common/util`'de. Her modül sorgusundan hemen önce çağırır, çünkü kaçış karakteri ile sorgudaki `ESCAPE` ifadesi bir çifttir |
| Küçük harf | **Yalnızca cache anahtarında** (`Locale.ROOT`, amaç tutarlılık). DB sorgusuna metin olduğu gibi gider. Kolonlar `utf8mb4_unicode_ci`, zaten büyük/küçük harfe duyarsız, bu yüzden `LOWER()` gerekmez |

Türkçe İ/ı neden önemli: `"IŞIK".toLowerCase()` İngilizce kuralla `"işik"`, Türkçe kuralla `"ışık"` olur, iki kuralın da bir yerde hatası var. Eşleştirmeyi DB collation'ına bırakmak bu sorunu Java tarafından çıkarır.

### 11. Güvenlik

`GET /api/search/**` → `SecurityConfig`'te `permitAll`. Anonim aramalarda kullanıcıya özel alan yok, bu yüzden cache paylaşılabilir.

## Modül bazında gereken değişiklikler (taslak)

| Modül | Quick search (yeni) | Full search |
|---|---|---|
| movie | `MovieSearchResult` + projeksiyon sorgusu | `search` yeni sıralamayla yeniden yazılır; `MovieController`'daki `?search=` dalı ve `searchByTitle` kaldırılır |
| series | `SeriesSearchResult` + projeksiyon sorgusu | Aynı (`SeriesController` `?search=` dalı kaldırılır) |
| community (Portal) | `PortalSearchResult` (ACTIVE + ARCHIVED) | Yeni `search` → `PortalSummaryResponse` (ACTIVE + ARCHIVED) |
| blog | `BlogSearchResult` (yalnız PUBLISHED) | Yeni `search` → `BlogSummaryResponse` |
| common/util | LIKE kaçış aracı | — |
| search (yeni) | `SearchHit`, `QuickSearchResponse`, `SearchService(+Impl)`, `SearchController` | Aynı controller'da tür başına uç |

## Açık sorular (2026-10-15'te karara bağlandı)

1. ~~Quick search limiti~~ → **Karar: sabit 5, `limit` parametresi yok** (YAGNI; gerçekte tür başına 2-4 eşleşme bekleniyor, limit yalnızca tavan. Frontend ihtiyaç duyarsa sonradan eklenir).
2. ~~Arşivli portallar~~ → **Karar: quick ve full search ACTIVE + ARCHIVED gösterir**, HIDDEN asla. Ayrı "önce aktifler" sıralaması yok.
3. ~~Kısmi sonuçta "eksik" bilgisi~~ → **Karar: frontend'e gitmez** (A). Yanıt şekli her zaman aynı; zaman aşımı yalnızca backend loguna (WARN, hangi tür + süre) yazılır. İzleme ihtiyacı log ile karşılanır; frontend isterse sonradan geriye uyumlu bir alan eklenebilir.
4. ~~Eski `?search=` uçları~~ → **Karar: admin paneli de yeni `/api/search/...` uçlarına geçer; Movie/Series controller'larındaki `?search=` dalı ve `searchByTitle` sorguları kaldırılır** (B). Admin paneli deneme amaçlı, admin aramasında taslak blog / HIDDEN portal görünmemesi kabul edildi.
   - **Uygulanan (2026-10-16):** Önce "genişlet, sonra daralt" sırası önerildi, ama kullanıcı admin'in arada filtresiz kalmasını kabul etti. Bu yüzden eski `search`/`searchByTitle` ve `?search=` dalları full search ile aynı commit'te silindi. Frontend admin (`admin/entities/entities.api.js` → `listEntities`) yeni uçlara taşınana kadar admin'deki film/dizi araması filtrelemez.
   - Not: admin'de Person/Character listeleri de `search` gönderiyor ama backend bu parametreyi hiç tanımıyordu (mevcut sessiz hata). Geçişle birlikte kendiliğinden çözülür.

Tüm açık sorular kapandı.

## Sonraya bırakılanlar

- **Person ve Character araması** (2026-10-15'te kapsamdan çıkarıldı; öncelik Movie/Series/Portal/Blog). Eklemek için desen hazır: `person/` modülünde `PersonSearchResult`/`CharacterSearchResult` + `name` üzerinde tek alanlı quick search sorgusu (dilden bağımsız), full search'te `PersonSummaryResponse`/`CharacterResponse`.

- **Yapımla ilgili içerik kutusu:** Arama tek bir yapıma tam uyuyorsa üstte "Dune ile ilgili: bloglar, oyuncular, portal" gösterilir. `BlogService.findRelatedForProduction` zaten var. Asıl zor kısım "tam uyuyor"un tanımı.
- Hat kotası (bulkhead), sorun görülürse.
- Portal'daki `PortalQueryServiceImpl.likePattern` kopyasını ortak araca taşımak.
- ~~Türkçe I/ı~~ → **ÇÖZÜLDÜ (2026-10-16, seçenek B): iki tarafta ı → i katlaması** (`common/util/SearchFold`).
  - **Sorun:** `utf8mb4_unicode_ci` I=i=İ, ü=u, ş=s eşler ama noktasız `ı`'yı ayrı harf sayar. Bu yüzden "ışık"/"IŞIK" aramaları "Işık"ı bulamıyordu.
  - **Çözüm:** Girdi Java'da `SearchFold.fold` ile, aranan kolonlar JPQL'de `replace(kolon, 'ı', 'i')` ile katlanır. Cache anahtarı da katlanır, yani "IŞIK" ve "ışık" aynı notu kullanır.
  - **Gerçek veriyle doğrulandı:** "Mulholland Çıkmazı" filmi çıkmazı/ÇIKMAZI/cikmazi ile, "Gözler Kapalı, Maskeler Açık" blogu kapalı/KAPALI/acik ile bulunuyor. "interstellar" hâlâ "Interstellar"ı buluyor.
  - **Elenen seçenekler:**
    - A) Olduğu gibi bırakmak.
    - `utf8mb4_tr_0900_ai_ci`: I/ı'yı çözer ama İngilizce I/i'yi bozar.
    - C) Ayrı "aranabilir başlık" kolonu: en sağlam yol ama migration + senkron yükü getirir. Büyüme merdiveninde yeniden değerlendirilir.
  - **Bedel:** Her satırda `replace()` çalışır. Tam tarama zaten yapıldığı için bugün ihmal edilebilir.
  - **Dağıtım notu:** Cache anahtarının anlamı değiştiği için bu sürüm devreye alınırken `fandoom:search:quick*` anahtarları temizlenmeli. Aksi halde eski kodun yazdığı "boş" cevaplar 1 saate kadar okunur.
- **Frontend:** Arama kutusuna debounce (~300ms).
- CLAUDE.md'ye `search/` bölümü eklenmesi (kullanıcıyla birlikte; CLAUDE.md'nin genel güncellemesi ayrıca ele alınacak).
