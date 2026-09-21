# Portal işi — devam notları (2026-09-21)

Sözleşme kaynağı: kullanıcının yapıştırdığı "Portal katmanı" görevi (B1/B2/B3). Bu dosya yeni session'ın kaldığı yerden devam etmesi içindir.

## Durum
- **B1, B2, B3 + güvenlik düzeltmeleri kodlandı** (commit YOK, working tree'de).
- **Son tam koşu YEŞİL: 482 test, 0 fail, 0 hata** (scratch şemada, agent raporu). `ThreadPortalConcurrencyTest` düzeltme sonrası 3/3 yeşil.
- Agent'ın raporuna göre `fandoom_b1_scratch` silindi (yalnız `fandoom` kaldı). İstersen `SHOW DATABASES LIKE 'fandoom%'` ile doğrula.
- Koşuda bulunan ek gerçek bug düzeltildi: `applyDelete` thread'i kilitsiz okuyordu; eşzamanlı taşıma+silmede sayaçlar kayıyordu → artık `SELECT ... FOR UPDATE` (`findEditableById/BySlug(…, true)`).

## Önce yapılacaklar
1. **`portal_migration.sql`'i** canlıya uygulamadan önce yeni build'in İLK açılışından ÖNCE çalıştır (yoksa Hibernate `portal_id`'yi NOT NULL ekleyip mevcut satırları bozar). Script hiç çalıştırılmadı.

## Uygulanan güvenlik düzeltmeleri (denetim bulguları)
HIDDEN portal tag sızıntısı (`findTrending`, `countByTagAndThread_Status`); create deadlock (incrementThreadCount INSERT'ten önce); çift sayaç düşümü (`markDeletedIfPublished`, taşımada PESSIMISTIC_WRITE); HIDDEN portaldaki thread'e sahip PATCH/DELETE 404 + `getRefsByIds` HIDDEN'da null + yorum silme/like'ta HIDDEN kontrolü; `PortalUpdateRequest` blank ad reddi, `ThreadPatchRequest.portalSlug` blank→null.

## Kullanıcı kararları (hepsi kapandı)
- **A. UYGULANDI** (`GlobalExceptionHandler`): bozuk JSON/okunamayan gövde -> 400; 405 (Allow başlığıyla) / 415 / 406 / 404 -> kendi durum kodlarıyla; 500 + ERROR stacktrace yok. Test: `GlobalExceptionHandlerFrameworkErrorsTest`.
- **B. UYGULANDI** (`CommunityRateLimitFilter`): portal join (POST) + leave (DELETE) aynı bucket, userId bazlı 60/saat. Test: `CommunityRateLimitFilterPortalTest`.
- **C. UYGULANDI**: ARCHIVED portalda thread PATCH (moderatör/admin hariç), yorum ekleme, thread like/bookmark, yorum like -> 400 (`ThreadRepository.isInArchivedPortal`). DELETE, unlike/unbookmark, portaldan ayrılma serbest.
- **D. UYGULANDI (öneriyle, Thread'e özgü)**: `ThreadServiceImpl.threadSlug` saf-sayısal sonuca `-t` ekler ("2024" -> "2024-t"). `common/SlugGenerator`'a DOKUNULMADI — Movie/Series slug'ları ("1917") etkilenmesin diye. Eski saf-sayısal slug'lı thread'ler (varsa) yalnız id ile erişilir.
- **E. UYGULANDI (öneriyle)**: anonim thread listesi/cursor ilk sayfa cache'i `productionSlug` verildiğinde devre dışı (`#productionSlug == null`). Not: `?tags=<rastgele>` için aynı key çeşitliliği hâlâ var (tag'ler normalize ediliyor ama sınırsız); istenirse aynı koşula eklenebilir.
- **Diğer varsayımlar:** trending eşitlik sırası `sortOrder→id` (spec `id` demişti); taşımada `productionSlug` tutarlılığı kontrol edilmiyor (yalnız create'te); `/me/portals?sort=activity` = trending skoru.

## Teslimatlar (tamamlandı)
1. `docs/portal-api-contract.md` kod + A–E ile senkronlandı (HIDDEN/ARCHIVED davranışları, rate limit, hata tablosu, numeric-slug, cache, Sapmalar #12/#15/#16/#20).
2. **CLAUDE.md'ye "Portal" bölümü + modül ağacı satırları eklendi.**
3. Migration doğrulaması (madde 8): `portal_migration.sql` scratch DB'de (eski şemalı thread/series/movie/comment + 7 thread) iki kez çalıştırıldı — hatasız, idempotent; backfill doğru (bağlı yapım -> portal, bağsız/null -> `genel-sohbet`), sayaçlar gerçek değere eşit, `portal_id` NOT NULL, `fk_thread_portal` + 4 indeks var, seed 9 portal / 3 bağ (DB'de olmayan yapımlar rapor sorgusunda listelendi). Entity indeks/uk adları migration'la birebir aynı. GERÇEK canlı DB'de çalıştırmak hâlâ kullanıcıya ait (yedek al, uygulamayı durdur).
4. Tam test paketi (scratch şemada, DB env'li): **491 test, 0 fail, 0 hata, BUILD SUCCESS** (A–E değişiklikleri dahil). Scratch şemalar silindi (yalnız `fandoom` kaldı).
5. **Migration YEREL `fandoom` DB'sinde ÇALIŞTIRILDI (2026-09-21).** Yedek: `D:\Kullanıcılar\Desktopandoom-backupsandoom_pre_portal_migration_20260921_145301.sql`. Durum: uygulama migration'dan ÖNCE yeni build'le açılmıştı; Hibernate portal tablolarını açıp `thread.portal_id`'yi NOT NULL + 0 ile eklemişti (FK/seed yok). Bu yüzden script iki noktada dayanıklı hale getirildi: (a) ADIM 3a-2: NOT NULL+0'lı `portal_id`'ler NULL'a çevrilip backfill'e sokulur; (b) `portal_production` seed INSERT'leri `created_at/updated_at` yazmaz (Hibernate tablosunda bu kolonlar yok). Sonuç: 9 portal, 8 thread -> `genel-sohbet`, `thread_count` gerçek değere eşit, `portal_id` NOT NULL, `fk_thread_portal` var, yetim yok, kopya indeks yok, 8 yapım bağı (`the-bear` DB'de olmadığı için atlandı). Başka bir ortamda (prod vb.) çalıştırılırsa yine yedek al + uygulamayı durdur.
6. Commit: ATILMADI (çalışma kopyasında portal dışı, önceden var olan değişiklikler de karışık; kullanıcı onayı bekliyor). Attribution: `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`.

## FE istek listesi (2026-09-21, ikinci tur)
- **Gerekli — `accentColor`: UYGULANDI.** `Portal.accentColor` (nullable `VARCHAR(7)`), create/PATCH (PATCH'te null=değişmedi, `""`=kaldır), Summary/Detail/Admin yanıtlarında; yalnız `^#[0-9A-Fa-f]{6}$` doğrulanır. Gradyan FE'de. `portal_migration.sql`'e idempotent `accent_color` adımı eklendi ve yerel `fandoom`'a uygulandı.
- **Hata — var olmayan thread id/slug'ta 500: DÜZELTİLDİ.** Kök neden: Redis erişilemezken `@Cacheable(sync=true)` okuması loader'dan önce `RedisConnectionFailureException` fırlatıyor; `CacheErrorHandler` bu yolda yetmiyor, loader'ın 404'ü de 500'e dönüyordu (var olanlar 200). `FailSafeCache` + `FailSafeRedisCacheManager` (RedisConfig). Etki tüm `sync=true` cache'li okumalar (Movie/Series/Blog/Thread). Redis çalışırken davranış değişmedi.
- **Yakın — Megathread: YAPILMADI (tasarım kararı gerekiyor).** Öneri: `Portal.pinnedThreadId` (nullable Long, portal başına TEK sabit thread) + `PUT/DELETE /api/community/portals/{slug}/pinned-thread` (MODERATOR/ADMIN; thread o portalın ve PUBLISHED olmalı) + `PortalDetailResponse.pinnedThread` (özet). Thread silinince/taşınınca/HIDDEN olunca sabitleme otomatik düşmeli.
- **Sonra — üye avatar önizlemesi, `lastViewedAt`: YAPILMADI** (istekte "sonra").

## Test ortamı notu
DB env'i yok (Access denied'lı 60 baseline hata bu yüzden). `.idea/workspace.xml` env'iyle ayrı scratch şemada koşuluyor, iş bitince şema silinir.
