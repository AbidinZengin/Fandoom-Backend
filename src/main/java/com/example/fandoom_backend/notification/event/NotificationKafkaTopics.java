package com.example.fandoom_backend.notification.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

// Notification modülünün yayınladığı topic'ler. Topic'i yayınlayan modül tanımlar; yoksa açılışta oluşturulur.
@Configuration
public class NotificationKafkaTopics {

    public static final String NOTIFICATION_CREATED = "notification.created";

    @Bean
    NewTopic notificationCreatedTopic() {
        return TopicBuilder.name(NOTIFICATION_CREATED).partitions(3).replicas(1).build();
    }
}
