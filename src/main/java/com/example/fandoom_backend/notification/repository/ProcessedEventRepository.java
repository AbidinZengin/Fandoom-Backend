package com.example.fandoom_backend.notification.repository;

import com.example.fandoom_backend.notification.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    // Hızlı yol: bu grup bu event'i daha önce işledi mi. Asıl güvence UNIQUE kısıtıdır (eşzamanlı kopyalar için).
    boolean existsByConsumerGroupAndEventId(String consumerGroup, String eventId);
}
