package com.example.fandoom_backend.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FailSafeCacheTest {

    private final Cache delegate = mock(Cache.class);
    private final FailSafeCache cache = new FailSafeCache(delegate);

    @Test
    void redisDown_loaderRunsDirectly_andItsBusinessExceptionIsWrappedForSpringToUnwrap() throws Exception {
        when(delegate.get(eq("k"), any(Callable.class))).thenThrow(new RedisConnectionFailureException("down"));
        IllegalStateException business = new IllegalStateException("404 gibi");

        assertThatThrownBy(() -> cache.get("k", () -> { throw business; }))
                .isInstanceOf(Cache.ValueRetrievalException.class)
                .hasCause(business);
    }

    @Test
    void redisDown_loaderSucceeds_returnsItsValue() {
        when(delegate.get(eq("k"), any(Callable.class))).thenThrow(new RedisConnectionFailureException("down"));

        assertThat(cache.<String>get("k", () -> "değer")).isEqualTo("değer");
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisFailsAfterLoaderCompleted_loaderIsNotRunAgain() {
        AtomicInteger calls = new AtomicInteger();
        // delegate loader'ı çalıştırır (DB okuması), sonra sonucu yazarken bağlantı kopar
        when(delegate.get(eq("k"), any(Callable.class))).thenAnswer(inv -> {
            ((Callable<Object>) inv.getArgument(1)).call();
            throw new RedisConnectionFailureException("put sırasında koptu");
        });

        String value = cache.get("k", () -> "v" + calls.incrementAndGet());

        assertThat(value).isEqualTo("v1");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void valueRetrievalExceptionFromDelegate_isPropagatedUntouched() {
        Cache.ValueRetrievalException original =
                new Cache.ValueRetrievalException("k", () -> null, new IllegalStateException("x"));
        when(delegate.get(eq("k"), any(Callable.class))).thenThrow(original);

        assertThatThrownBy(() -> cache.get("k", () -> "asla çağrılmaz")).isSameAs(original);
    }

    @Test
    void otherOperationsDelegate() {
        when(delegate.get("k")).thenReturn(new SimpleValueWrapper("x"));
        assertThat(cache.get("k").get()).isEqualTo("x");
    }
}
