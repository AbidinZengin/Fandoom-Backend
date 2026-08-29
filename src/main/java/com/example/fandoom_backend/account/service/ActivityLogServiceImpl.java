package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.entity.ActivityType;
import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.account.entity.UserActivityLog;
import com.example.fandoom_backend.account.repository.UserActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityLogServiceImpl implements ActivityLogService {

    private final UserActivityLogRepository userActivityLogRepository;

    @Override
    @Transactional
    public void record(Long userId, ActivityType type, Long itemId, SavedItemType itemType) {
        userActivityLogRepository.save(UserActivityLog.builder()
                .userId(userId)
                .activityType(type)
                .itemId(itemId)
                .itemType(itemType)
                .build());
    }
}
