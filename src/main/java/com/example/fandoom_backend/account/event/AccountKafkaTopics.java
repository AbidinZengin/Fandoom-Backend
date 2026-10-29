package com.example.fandoom_backend.account.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

// Account modülünün yayınladığı topic'ler. Topic'i yayınlayan modül tanımlar; uygulama açılışında yoksa oluşturulur.
@Configuration
public class AccountKafkaTopics {

    public static final String FOLLOW_EVENTS = "account.follow-events";

    @Bean
    NewTopic followEventsTopic() {
        // 3 partition: farklı ilişkiler paralel işlenebilir. 1 replika: tek brokerlı yerel ortam.
        return TopicBuilder.name(FOLLOW_EVENTS).partitions(3).replicas(1).build();
    }
}
