# k6 Yük Testleri

Kurulum: https://grafana.com/docs/k6/latest/set-up/install-k6/ (`k6 version` ile kontrol et).

```
k6/
├── utils/auth.js                     # login / token yardımcıları
├── scenarios/feed_load_test.js       # okuma/cache: GET /api/community/feed/cursor (100 VU, 10 sn)
├── scenarios/create_thread_test.js   # yazma/evict: POST /api/community/threads (10 VU, 10 sn)
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
