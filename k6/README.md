# k6 Yük Testleri

Kurulum: https://grafana.com/docs/k6/latest/set-up/install-k6/ (`k6 version` ile kontrol et).

```
k6/
├── utils/auth.js                     # login / token yardımcıları
├── scenarios/feed_load_test.js       # okuma/cache: GET /api/community/feed/cursor (100 VU, 10 sn)
├── scenarios/create_thread_test.js   # yazma/evict: POST /api/community/threads (10 VU, 10 sn)
├── scenarios/feed_cache_vs_db_test.js # aynı uçta cache HIT (anonim) vs DB yolu (girişli) karşılaştırması
├── db/seed_threads.sql               # 50.000 sentetik thread ekler (yük/EXPLAIN için)
├── db/explain_thread_queries.sql     # feed sorgularının indeks kullanımı (filesort OLMAMALI)
├── db/cleanup_seed.sql               # seed + k6 yazma testinin bıraktığı verileri siler
├── run_all.sh / run_all.bat          # ikisini sırayla çalıştırır
```

## Çalıştırma

```bash
# Sadece okuma testi (kimlik bilgisi gerekmez)
k6 run k6/scenarios/feed_load_test.js

# Yazma testi — aşağıdaki kimlik seçeneklerinden biri
k6 run -e LOAD_USERNAME=loadtest -e LOAD_PASSWORD=... k6/scenarios/create_thread_test.js
k6 run -e TOKEN=eyJ...                                  k6/scenarios/create_thread_test.js
k6 run -e LOAD_USERS="u1:p1,u2:p2"                      k6/scenarios/create_thread_test.js

# Hepsi
BASE_URL=http://localhost:8080 LOAD_USERNAME=loadtest LOAD_PASSWORD=... ./k6/run_all.sh
```

`BASE_URL` varsayılanı `http://localhost:8080`.

## Bilinmesi gerekenler

- **Test kullanıcısı e-postası doğrulanmış olmalı** (login `EmailNotVerifiedException` fırlatır).
- **Login rate limit'i** (IP başına 10/dk): token'lar `setup()`'ta bir kez alınır, VU başına login yapılmaz.
- **Thread yazma limiti** (`CommunityRateLimitFilter`): kullanıcı başına 10/saat, in-memory. Tek kullanıcıyla
  test çok hızlı 429'a düşer; 429'lar `rate_limited` sayacında raporlanır, gecikme eşiği yalnızca 201'ler
  üzerinden (`create_thread_duration`) ölçülür. Daha fazla gerçek yazma için `LOAD_USERS` ile çok kullanıcı
  ver veya limit sıfırlamak için uygulamayı yeniden başlat.
- **Yazma testi DB'ye veri bırakır** (başlıkları `[k6]` ile başlar) — prod'a karşı çalıştırma.
- **Feed testinde cache**: anonim istek ilk sayfada Redis cache HIT yolunu ölçer; cursor'lu sayfalar
  cache'lenmez. Katalogda tek sayfadan az thread varsa `nextCursor` boş döner ve cursor eşikleri boş kalır.
- Cache evict etkisini görmek için iki testi **aynı anda, iki terminalde** çalıştır.

## Gerçekçi ölçüm akışı (DB + cache)

Boş bir tabloda hem `EXPLAIN` hem yük testi anlamsızdır (optimizer tam-tarama seçer, feed boş döner). Sırayla:

```bash
# 1) Sentetik veri (dev DB; prod'a karşı ÇALIŞTIRMA). Parola MYSQL_PWD env'iyle, komut satırına yazılmaz.
MYSQL_PWD=... mysql -h localhost -u root fandoom < k6/db/seed_threads.sql

# 2) İndeks doğrulaması: hiçbir satırda "Using filesort" olmamalı (son "KANIT" bloğu hariç).
MYSQL_PWD=... mysql -t -h localhost -u root fandoom < k6/db/explain_thread_queries.sql

# 3) Uygulamayı başlat (Redis + MySQL açık), sonra:
k6 run k6/scenarios/feed_load_test.js
k6 run -e LOAD_USERNAME=... -e LOAD_PASSWORD=... k6/scenarios/feed_cache_vs_db_test.js

# 4) Temizlik (seed + '[k6]' başlıklı yazma testi verileri)
MYSQL_PWD=... mysql -h localhost -u root fandoom < k6/db/cleanup_seed.sql
```

- **Kaynak uygulama**: uygulamayı `target/`'tan çalıştırıyorsan (`spring-boot:run`) aynı anda `mvn test` çalıştırma
  (derleme çıktısını ezer). IDE'de çalışan eski bir örnek varsa yeni uçlar (`/feed/cursor`) onda olmayabilir:
  güncel kodu ayrı bir portta başlatıp `-e BASE_URL=http://localhost:8091` ile hedefle.
- **Entegrasyon testleri ile çakışma**: `HotScoreJobIntegrationTest` gibi `@DataJpaTest`'ler dev DB'de başka PUBLISHED
  thread olmadığını varsayar; seed verisi varken bu testleri çalıştırma (önce `cleanup_seed.sql`).

## Bulgu: keyset tie-breaker `id ASC` olmalı (2026-09-19)

`ORDER BY hot_score DESC, id DESC` (ilk yazılan hali) 50.000 satırlık tabloda **her feed sorgusunda `Using filesort`**
verdi ve optimizer yanlış indeksi seçti: InnoDB ikincil indekslere PK'yı örtük **ASC** ekler, ters yönde sıralama
indeksi kullandırmaz. `id ASC` ile 10 sorgunun 10'u ilgili özel indeksi (`idx_thread_status_hot`,
`..._surface_created`, `..._production_hot` vb.) filesort'suz kullandı. Uygulama bu yüzden `(alan DESC, id ASC)` +
`id > :lastId` kullanır (`KeysetSpecification`). `explain_thread_queries.sql`'in son bloğu ters yönü kanıtlar.

## Ölçüm sonuçları (2026-09-19, yerel makine, 50.000 thread, Redis + MySQL localhost)

Mutlak süreler yerel makineye özgüdür; anlamlı olan oranlar ve eşik davranışıdır.

| Test | Sonuç |
|---|---|
| `feed_load_test.js` (100 VU, sıcak JVM) | ilk sayfa (cache HIT) p95 ≈ 9 ms, cursor'lu sayfa (DB) p95 ≈ 20 ms, ~1.650 istek/sn, hata 0 |
| Aynı test, **cache boşaltılmış** (JVM sıcak) | ilk sayfa max 178 ms: `sync = true` sayesinde 100 VU'nun eşzamanlı cache miss'i DB'ye yığılmadı |
| Aynı test, **JVM soğuk** ilk koşu | ilk sayfa p99 ≈ 2,2 s (max 2,4 s): cache değil, JVM/Hibernate ısınması. Eşik p(99)<300 ms bu ilk koşuda kırılır; ısınmış uygulamada geçer |
| `feed_cache_vs_db_test.js` (50 VU) | cache HIT medyan 7,6 ms / p95 12,5 ms; DB yolu medyan 13,2 ms / p95 18 ms (~1,7x). Fark küçük çünkü keyset+indeksli DB yolu zaten hızlı |
| `create_thread_test.js` (10 VU) | 10 yazma (kullanıcı başına 10/saat limiti), ~185 ms; sonrası 429 (beklenen) |

`HotScoreJob` (50.000 thread, 5 pencere): 494 ms. Bu, değeri değişmeyen satırlar için (MySQL yazmayı atlar);
500.000 satırlık tam yeniden yazımın ~78 sn sürdüğü ayrı ölçümle bulundu (indeks bakımı baskın).

## Bulgu: geleceğe tarihli `created_at` HotScoreJob'u düşürüyordu (2026-09-19)

Seed verisinin bazı `created_at` değerleri job'ın "şimdi"sinden 2-3 saat ileride kaldığında `TIMESTAMPDIFF` `-2` verir,
`POW(0, 1.5) = 0` olur ve MySQL strict modda `Division by 0` ile TÜM pencere (job) düşerdi. Üretimde saat kayması veya
hatalı veriyle de olabilirdi. SQL artık yaşı `GREATEST(..., 0)` ile kırpar (regresyon testi:
`HotScoreJobIntegrationTest.futureDatedThread_...`).
