// k6/scenarios/feed_cache_vs_db_test.js
// CACHE HIT vs DB YOLU KARŞILAŞTIRMASI — GET /api/community/feed/cursor?size=20&sort=hot (aynı uç, aynı yük)
//
//   cache_hit : ANONİM istek  -> viewerId == null -> Thread ilk sayfası Redis'ten gelir.
//   db_path   : GİRİŞLİ istek -> viewerId != null -> cache BYPASS (isLiked/isBookmarked kullanıcıya özel),
//                                her istek DB'ye (keyset sorgusu + toplu zenginleştirme) iner.
// İki senaryo ARDIŞIK koşar (aynı anda değil) ki birbirinin yükünü kirletmesin.
//
// Önkoşul: anlamlı sonuç için thread tablosu dolu olmalı (k6/db/seed_threads.sql). Redis çalışıyor olmalı.
//
// Çalıştırma (giriş için k6/README.md'deki kimlik seçeneklerinden biri):
//   k6 run -e LOAD_USERNAME=... -e LOAD_PASSWORD=... k6/scenarios/feed_cache_vs_db_test.js
//   k6 run -e TOKEN=eyJ...                             k6/scenarios/feed_cache_vs_db_test.js
//
// Çıktıda karşılaştırılacak metrikler: http_req_duration{path:cache_hit} ve http_req_duration{path:db_path}.
// Eşikler bilinçli gevşek (makineye bağlı); asıl değer iki p(95) arasındaki orandır.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, getTokens, authHeaders } from '../utils/auth.js';

const URL = `${BASE_URL}/api/community/feed/cursor?size=20&sort=hot`;

export const options = {
  scenarios: {
    cache_hit: { executor: 'constant-vus', vus: 50, duration: '10s', exec: 'cacheHit', startTime: '0s' },
    db_path: { executor: 'constant-vus', vus: 50, duration: '10s', exec: 'dbPath', startTime: '12s' },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{path:cache_hit}': ['p(95)<150'],
    'http_req_duration{path:db_path}': ['p(95)<800'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  // Login rate limit'i (IP başına 10/dk) yüzünden token'lar burada BİR KEZ alınır.
  return { token: getTokens()[0] };
}

export function cacheHit() {
  const res = http.get(URL, { tags: { name: 'feed_first_anonymous', path: 'cache_hit' } });
  check(res, { 'cache_hit: 200': (r) => r.status === 200 });
  sleep(0.1);
}

export function dbPath(data) {
  const res = http.get(URL, { headers: authHeaders(data.token), tags: { name: 'feed_first_authenticated', path: 'db_path' } });
  check(res, { 'db_path: 200': (r) => r.status === 200 });
  sleep(0.1);
}
