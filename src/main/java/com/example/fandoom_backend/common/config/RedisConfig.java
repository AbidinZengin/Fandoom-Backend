package com.example.fandoom_backend.common.config;

import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;

// Redis tabanlı Spring Cache. Kurallar:
//  - Tüm cache'ler için TEK, global TTL (özel/çoklu TTL bilinçli olarak YOK).
//  - Okuma metodları @Cacheable(sync = true) ile korunur (cache stampede: aynı anahtar için
//    eşzamanlı miss'lerde tek çağrı DB'ye gider). NOT: RedisCache'in sync kilidi instance-içidir
//    (JVM-local), dağıtık değil — N instance'ta en kötü ihtimalle N eşzamanlı yükleme olur.
//  - Yazma metodları @CacheEvict ile temizler; transactionAware() sayesinde eviction transaction
//    COMMIT'inden sonra çalışır (commit öncesi silinirse araya giren bir okuma eski veriyi geri
//    yazıp bayat bırakabilirdi).
//  - Cache'lenen değerler entity değil DTO (record) — entity'ler hiçbir zaman cache'e girmez.
@Configuration
@EnableCaching
public class RedisConfig {

    static final Duration DEFAULT_TTL = Duration.ofHours(1);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .prefixCacheNameWith("fandoom:")
                // Cache'lenen metodlar null dönmez (yoksa 404 exception'ı); null'lar cache'e yazılmaz.
                .disableCachingNullValues()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(cacheValueSerializer()));

        // RedisCacheManager.builder() bir alt sınıf üretemez; her cache FailSafeCache ile sarılsın diye doğrudan kuruluyor
        // (builder'ın varsayılan writer'ı ile aynı: nonLocking). Redis kesintisinde sync okuma DB'ye düşer (bkz. FailSafeCache).
        FailSafeRedisCacheManager manager = new FailSafeRedisCacheManager(
                RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory), defaultConfig);
        manager.setTransactionAware(true);
        return manager;
    }

    static final class FailSafeRedisCacheManager extends RedisCacheManager {
        FailSafeRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultConfig) {
            super(cacheWriter, defaultConfig);
        }

        // transactionAware ise super, FailSafeCache'i TransactionAwareCacheDecorator ile sarar (evict commit sonrası).
        @Override
        protected Cache decorateCache(Cache cache) {
            return super.decorateCache(new FailSafeCache(cache));
        }
    }

    // Değerler okunabilir JSON olarak saklanır ("@class" tip bilgisiyle). Jackson 3 tabanlı
    // GenericJacksonJsonRedisSerializer: GenericJackson2JsonRedisSerializer'ın (Jackson 2,
    // SDR 4.x'te @Deprecated) halefi — projede Jackson 2 yalnızca jjwt üzerinden transitif geliyor.
    // Default typing DTO record'ları (final) ve PageResponse<T> gibi generic zarfları da
    // tipiyle geri okuyabilsin diye açık; deserialize edilebilecek sınıflar allow-list ile
    // sınırlı (polymorphic deserialization gadget saldırılarına karşı).
    static RedisSerializer<Object> cacheValueSerializer() {
        BasicPolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.example.fandoom_backend.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.math.")
                .allowIfSubType("java.lang.")
                .build();
        return GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .build();
    }
}
