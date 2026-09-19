#!/usr/bin/env bash
# Tüm k6 senaryolarını sırayla çalıştırır. Bir senaryo threshold'u kaçırırsa
# yine de sonrakine geçilir; en sonda herhangi biri başarısızsa exit code 1 döner.
#
# Kullanım:
#   BASE_URL=http://localhost:8080 LOAD_USERNAME=loadtest LOAD_PASSWORD=... ./k6/run_all.sh
#   (create_thread_test.js için TOKEN / TOKENS / LOAD_USERS de kullanılabilir)
# Not: k6 süreç ortam değişkenlerini __ENV'e otomatik yansıtır, ayrıca -e gerekmez.
set -u
cd "$(dirname "$0")"

FAILED=0
for scenario in scenarios/feed_load_test.js scenarios/create_thread_test.js; do
  echo
  echo "=== ${scenario} ==="
  k6 run "${scenario}" || FAILED=1
done

echo
if [ "${FAILED}" -ne 0 ]; then
  echo "En az bir senaryo başarısız oldu (threshold veya hata)."
  exit 1
fi
echo "Tüm senaryolar başarıyla tamamlandı."
