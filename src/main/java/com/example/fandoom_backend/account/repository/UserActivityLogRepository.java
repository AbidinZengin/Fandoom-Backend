package com.example.fandoom_backend.account.repository;

import com.example.fandoom_backend.account.entity.ActivityType;
import com.example.fandoom_backend.account.entity.UserActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {
    long countByUserIdAndActivityType(Long userId, ActivityType activityType);
}
