// k6/utils/auth.js
// Yük testleri için ortak kimlik doğrulama yardımcıları.
//
// ÖNEMLİ — neden VU başına login YAPILMIYOR:
//   - AuthRateLimitFilter: /api/auth/login için IP başına 10 istek/dakika.
//     Her VU (veya her iterasyon) login olsaydı, test 429'larla dolardı ve
//     ölçtüğümüz şey login rate-limit'i olurdu. Bu yüzden token'lar test
//     başlamadan önce, `setup()` içinde BİR KEZ alınır.
//   - Login için e-posta doğrulaması gerekir (EmailNotVerifiedException) —
//     test kullanıcıları doğrulanmış olmalıdır.
//
// Token kaynakları (öncelik sırasıyla):
//   1. TOKENS       -> "jwt1,jwt2,..." (hazır Bearer token'lar; login çağrısı yapılmaz)
//   2. TOKEN        -> tek bir hazır Bearer token
//   3. LOAD_USERS   -> "kullanici1:sifre1,kullanici2:sifre2" (her biri için 1 login)
//   4. LOAD_USERNAME + LOAD_PASSWORD -> tek kullanıcı (1 login)

import http from 'k6/http';
import { fail } from 'k6';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

/**
 * POST /api/auth/login — başarılıysa JWT string'i döner, değilse testi durdurur.
 * Yalnızca setup() içinde çağrılmalı (bkz. dosya başındaki rate-limit notu).
 */
export function login(usernameOrEmail, password) {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ usernameOrEmail, password }),
    { headers: JSON_HEADERS, tags: { name: 'auth_login' } },
  );

  if (res.status !== 200) {
    fail(`Login başarısız (${usernameOrEmail}): HTTP ${res.status} — ${res.body}`);
  }

  const token = res.json('token');
  if (!token) {
    fail(`Login yanıtında token yok (${usernameOrEmail}): ${res.body}`);
  }
  return token;
}

/**
 * Test için kullanılacak tüm token'ların listesini döner (en az 1 eleman).
 * setup() içinde çağır, dönen diziyi data olarak VU'lara ilet.
 */
export function getTokens() {
  if (__ENV.TOKENS) {
    return splitList(__ENV.TOKENS);
  }
  if (__ENV.TOKEN) {
    return [__ENV.TOKEN];
  }

  if (__ENV.LOAD_USERS) {
    return splitList(__ENV.LOAD_USERS).map((pair) => {
      const idx = pair.indexOf(':');
      if (idx < 1) {
        fail(`LOAD_USERS formatı "kullanici:sifre,kullanici2:sifre2" olmalı, hatalı öğe: "${pair}"`);
      }
      return login(pair.slice(0, idx), pair.slice(idx + 1));
    });
  }

  if (__ENV.LOAD_USERNAME && __ENV.LOAD_PASSWORD) {
    return [login(__ENV.LOAD_USERNAME, __ENV.LOAD_PASSWORD)];
  }

  fail(
    'Kimlik bilgisi yok. Şunlardan birini ver: TOKENS, TOKEN, LOAD_USERS veya ' +
      'LOAD_USERNAME+LOAD_PASSWORD (bkz. k6/README.md).',
  );
}

/** Authorization + JSON header'larını üretir. */
export function authHeaders(token) {
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };
}

function splitList(value) {
  return value
    .split(',')
    .map((s) => s.trim())
    .filter((s) => s.length > 0);
}
