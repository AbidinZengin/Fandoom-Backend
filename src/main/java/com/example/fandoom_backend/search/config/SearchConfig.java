package com.example.fandoom_backend.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class SearchConfig {

    // Quick search'ün paralel kaynak sorguları için: her görev kendi sanal thread'inde (Java 21). Sorgular çoğunlukla
    // DB'yi beklediği için sanal thread ucuzdur. Not: eşzamanlılığı sınırlayan şey thread değil HikariCP havuzudur
    // (her quick search aynı anda 4 bağlantı ister) — bkz. docs/plans/2026-10-15-unified-search-design.md Karar 8.
    @Bean(destroyMethod = "close")
    public ExecutorService searchExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
