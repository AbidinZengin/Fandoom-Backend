package com.example.fandoom_backend.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// "Cache stampede korumasını sisteme kazı" kuralının otomatik bekçisi: projedeki HER @Cacheable
// metodu sync = true olmalı (ve sync ile birlikte Spring'in yasakladığı unless kullanılmamalı).
// Yeni bir modüle @Cacheable eklenip sync unutulursa bu test kırılır.
class CacheContractTest {

    @Test
    void everyCacheableMethodUsesSyncTrue() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Object.class));

        List<String> violations = new ArrayList<>();
        int cacheableCount = 0;
        for (BeanDefinition definition : scanner.findCandidateComponents("com.example.fandoom_backend")) {
            Class<?> type = Class.forName(definition.getBeanClassName());
            for (Method method : type.getDeclaredMethods()) {
                Cacheable cacheable = AnnotatedElementUtils.findMergedAnnotation(method, Cacheable.class);
                if (cacheable == null) {
                    continue;
                }
                cacheableCount++;
                String where = type.getSimpleName() + "#" + method.getName();
                if (!cacheable.sync()) {
                    violations.add(where + " -> sync = true değil");
                }
                if (!cacheable.unless().isEmpty()) {
                    violations.add(where + " -> sync ile unless birlikte kullanılamaz");
                }
            }
        }

        assertThat(cacheableCount).as("taranan @Cacheable sayısı (tarama çalışıyor mu?)").isGreaterThan(10);
        assertThat(violations).isEmpty();
    }
}
