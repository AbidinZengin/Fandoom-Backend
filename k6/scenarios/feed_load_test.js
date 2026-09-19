// k6/scenarios/feed_load_test.js
// OKUMA / CACHE TESTİ — GET /api/community/feed/cursor
//
// Anonim istek (Authorization yok) => viewerId == null => Thread okumaları Redis
// cache'e girer (bkz. CLAUDE.md "Redis Cache"). Yani ilk sayfa isteği cache HIT
// yolunu ölçer; cursor'lu sayfalar bilinçli olarak cache'lenmez (DB yolunu ölçer).
// İki yolu ayrı tag'lerle (page_type) raporlayıp threshold'larını ayrı koyuyoruz.
//
// Çalıştırma:
//   k6 run k6/scenarios/feed_load_test.js
//   k6 run -e BASE_URL=http://staging:8080 k6/scenarios/feed_load_test.js
//
// Beklenen değerler (http_req_duration):
//   - İlk sayfa (Redis cache HIT):  p(95) < 150 ms   (p(99) < 300 ms)
//   - Cursor'lu sayfa (DB, keyset): p(95) < 400 ms   (p(99) < 800 ms)
//   - Genel:                        p(95) < 300 ms
//   - Hata oranı (http_req_failed): < %1
//   Not: ilk saniyelerde cache soğuktur (sync=true sayesinde stampede olmaz ama
//   ilk istek DB'ye gider); eşikler bu ısınmayı tolere edecek şekilde p(95) alır.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL } from '../utils/auth.js';

export const options = {
  vus: 100,
  duration: '10s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<300'],
    'http_req_duration{page_type:first}': ['p(95)<150', 'p(99)<300'],
    'http_req_duration{page_type:cursor}': ['p(95)<400', 'p(99)<800'],
    checks: ['rate>0.99'],
  },
};

const FEED_URL = `${BASE_URL}/api/community/feed/cursor`;

export default function () {
  // 1) İlk sayfa — cursor yok, cache'lenen yol.
  const first = http.get(`${FEED_URL}?sort=hot&size=20`, {
    tags: { name: 'feed_cursor_first', page_type: 'first' },
  });

  const firstOk = check(first, {
    'feed ilk sayfa: status 200': (r) => r.status === 200,
    'feed ilk sayfa: content dizisi var': (r) => Array.isArray(r.json('content')),
  });

  // 2) İkinci sayfa — nextCursor varsa takip et (cache'siz DB yolu).
  //    Katalog boşsa / tek sayfaysa nextCursor null olur; sorun değil.
  if (firstOk) {
    const nextCursor = first.json('nextCursor');
    if (nextCursor) {
      const second = http.get(
        `${FEED_URL}?sort=hot&size=20&cursor=${encodeURIComponent(nextCursor)}`,
        { tags: { name: 'feed_cursor_next', page_type: 'cursor' } },
      );
      check(second, {
        'feed sonraki sayfa: status 200': (r) => r.status === 200,
      });
    }
  }

  sleep(0.1);
}
