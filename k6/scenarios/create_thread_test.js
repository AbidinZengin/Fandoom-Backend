// k6/scenarios/create_thread_test.js
// YAZMA / EVICT TESTİ — POST /api/community/threads
//
// Her başarılı yazma Thread cache'ini `@CacheEvict(allEntries = true)` ile
// temizler; yani bu test aynı zamanda evict maliyetini ölçer. Feed testiyle
// AYNI ANDA koşturulursa cache hit oranının yazmalar altında nasıl düştüğü
// görülebilir (iki terminalde paralel çalıştır).
//
// RATE LIMIT (CommunityRateLimitFilter): kullanıcı başına 10 thread / saat,
// in-memory. 10 VU ile 10 sn'de tek kullanıcı çok hızlı 429'a düşer. Bu yüzden:
//   - 201 (başarılı yazma) ve 429 (limit) İKİSİ de "beklenen" sayılır,
//     429'lar `rate_limited` sayacında ayrı raporlanır.
//   - Gecikme eşikleri yalnızca GERÇEK yazmalar (201) üzerinde ölçülür
//     (`create_thread_duration` Trend'i); 429 cevapları DB'ye hiç inmediği için ölçümü kirletirdi.
//   - Daha fazla gerçek yazma için birden çok kullanıcı ver:
//       -e LOAD_USERS="u1:pass1,u2:pass2,u3:pass3"  (kullanıcı başına ~10 thread/saat)
//     (Login rate limit'i IP başına 10/dk — en fazla ~10 kullanıcı ver.)
//   - Limit in-memory olduğundan, tekrar koşmadan önce uygulamayı yeniden başlat
//     veya bir saat bekle. Test verisi DB'de kalır (başlıklar "[k6]" ile başlar).
//
// Çalıştırma:
//   k6 run -e LOAD_USERNAME=loadtest -e LOAD_PASSWORD=... k6/scenarios/create_thread_test.js
//   k6 run -e TOKEN=eyJ... k6/scenarios/create_thread_test.js
//
// Beklenen değerler (http_req_duration / create_thread_duration, yalnızca 201 yazmalar):
//   - p(95) < 800 ms   (INSERT + tag'ler + cache evict + yazar zenginleştirme)
//   - p(99) < 1500 ms
//   - Beklenmeyen hata (201/429 dışı, 5xx dahil): < %1
//   Yazma ucu okumadan bilinçli olarak daha gevşek: transaction + evict içerir.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { BASE_URL, getTokens, authHeaders } from '../utils/auth.js';

// 201 (yazıldı) ve 429 (rate limit) beklenen sayılır; 429'lar aşağıda ayrıca sayılır.
http.setResponseCallback(http.expectedStatuses(201, 429));

const rateLimited = new Counter('rate_limited');
const created = new Counter('threads_created');
// Yalnızca başarılı (201) yazmaların süresi — 429'lar bu metriği kirletmez.
const createDuration = new Trend('create_thread_duration', true);

export const options = {
  vus: 10,
  duration: '10s',
  thresholds: {
    // Yalnızca 201'ler (özel Trend); http_req_duration 429'ları da içerdiği için kullanılmaz.
    create_thread_duration: ['p(95)<800', 'p(99)<1500'],
    // Hiç gerçek yazma olmadıysa (ör. hepsi 429/401) test yeşil görünmesin.
    threads_created: ['count>0'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
};

// Token(lar) test başlamadan önce BİR KEZ alınır (login rate-limit'i için).
export function setup() {
  return { tokens: getTokens() };
}

export default function (data) {
  // VU'ları token'lara dağıt (__VU 1'den başlar).
  const token = data.tokens[(__VU - 1) % data.tokens.length];

  const payload = JSON.stringify({
    surface: 'DISCUSSION',
    title: `[k6] Yük testi başlığı vu${__VU} it${__ITER} ${Date.now()}`,
    body: 'Bu thread k6 yük testi tarafından oluşturuldu. '.repeat(5),
    spoilerFlagged: false,
    tags: ['k6', 'yuk-testi'],
  });

  const res = http.post(`${BASE_URL}/api/community/threads`, payload, {
    headers: authHeaders(token),
    tags: { name: 'create_thread' },
  });

  if (res.status === 429) {
    rateLimited.add(1);
  } else if (res.status === 201) {
    created.add(1);
    createDuration.add(res.timings.duration);
  }

  check(res, {
    'thread: 201 veya 429': (r) => r.status === 201 || r.status === 429,
    'thread: 201 ise id dönüyor': (r) => r.status !== 201 || r.json('id') !== undefined,
  });

  sleep(0.5);
}
