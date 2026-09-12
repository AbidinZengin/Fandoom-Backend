package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.entity.ActivityType;
import com.example.fandoom_backend.account.entity.SavedItemType;

// Diğer modüller (blog/, ileride community/) bu interface'i inject edip
// doğrudan çağırır (DIP — CastServiceImpl'in MovieService/SeriesService
// inject etme deseniyle aynı yön, event-bus gibi yeni bir pattern YOK).
public interface ActivityLogService {
    void record(Long userId, ActivityType type, Long itemId, SavedItemType itemType);
    // community/'nin ProfileStats hesaplamasi (readBlogCount) icin — dogrudan
    // UserActivityLogRepository'ye cross-module erisim yerine.
    long countByUserIdAndType(Long userId, ActivityType type);
}
