package com.example.fandoom_backend.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.lang.NonNull;

import java.util.concurrent.Callable;

/**
 * {@code @Cacheable(sync = true)} yolunu Redis kesintisine karşı dayanıklı yapan decorator.
 *
 * <p>Sorun: sync okumada Spring {@code cache.get(key, loader)} çağırır ve Redis erişilemezse
 * {@code RedisConnectionFailureException} loader ÇALIŞMADAN fırlar. {@code CacheErrorHandler} bu yolda hatayı
 * yutsa da loader'ın fırlattığı iş exception'ı (ör. {@code ResourceNotFoundException} -> 404) ile birlikte
 * Redis hatası yayılıp isteği 500'e düşürüyordu (var olan kayıtlar 200, var olmayanlar 500).
 *
 * <p>Çözüm: Redis kaynaklı hata olursa loader doğrudan çalıştırılır; loader'ın exception'ı Spring'in beklediği
 * {@link Cache.ValueRetrievalException} ile sarılır (Spring bunu açıp özgün exception'ı fırlatır). Loader Redis
 * hatasından ÖNCE zaten tamamlandıysa (ör. sonuç yazılırken bağlantı koptu) tekrar çalıştırılmaz.
 * Redis erişilebilirken davranış aynıdır. Diğer işlemler (put/evict/get) mevcut {@code CacheErrorHandler}'a kalır.
 */
final class FailSafeCache implements Cache {

    private static final Logger log = LoggerFactory.getLogger(FailSafeCache.class);

    private final Cache delegate;

    FailSafeCache(Cache delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
        LoaderOutcome<T> outcome = new LoaderOutcome<>(valueLoader);
        try {
            return delegate.get(key, outcome);
        } catch (ValueRetrievalException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Cache '{}' okuma hatası, DB'ye düşülüyor (key={}): {}", getName(), key, e.toString());
            try {
                return outcome.resultOrCall();
            } catch (Exception loaderFailure) {
                throw new ValueRetrievalException(key, valueLoader, loaderFailure);
            }
        }
    }

    // loader'ın sonucunu/tamamlandığını izler: Redis hatası loader'dan SONRA gelirse aynı sonuç tekrar kullanılır.
    private static final class LoaderOutcome<T> implements Callable<T> {
        private final Callable<T> loader;
        private boolean done;
        private T result;

        LoaderOutcome(Callable<T> loader) {
            this.loader = loader;
        }

        @Override
        public T call() throws Exception {
            T value = loader.call();
            result = value;
            done = true;
            return value;
        }

        T resultOrCall() throws Exception {
            return done ? result : loader.call();
        }
    }

    @Override public @NonNull String getName() { return delegate.getName(); }
    @Override public @NonNull Object getNativeCache() { return delegate.getNativeCache(); }
    @Override public ValueWrapper get(@NonNull Object key) { return delegate.get(key); }
    @Override public <T> T get(@NonNull Object key, Class<T> type) { return delegate.get(key, type); }
    @Override public void put(@NonNull Object key, Object value) { delegate.put(key, value); }
    @Override public ValueWrapper putIfAbsent(@NonNull Object key, Object value) { return delegate.putIfAbsent(key, value); }
    @Override public void evict(@NonNull Object key) { delegate.evict(key); }
    @Override public boolean evictIfPresent(@NonNull Object key) { return delegate.evictIfPresent(key); }
    @Override public void clear() { delegate.clear(); }
    @Override public boolean invalidate() { return delegate.invalidate(); }
}
