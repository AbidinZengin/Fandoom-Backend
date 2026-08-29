package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.entity.ListType;
import com.example.fandoom_backend.account.entity.UserList;
import com.example.fandoom_backend.account.repository.UserListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// UserListServiceImpl.list() ve UserSavedItemServiceImpl.save() arasında
// paylaşılan "sistem listesi lazy oluşturma" mantığı — findByUserIdAndListType
// (...).orElseGet(() -> save(yeni)). WATCHLIST/READLIST silinemez/tipi
// değiştirilemez (bkz. UserListServiceImpl.delete/update).
@Component
@RequiredArgsConstructor
class SystemListRegistry {

    private final UserListRepository userListRepository;

    UserList resolve(Long userId, ListType listType) {
        return userListRepository.findByUserIdAndListType(userId, listType)
                .orElseGet(() -> userListRepository.save(UserList.builder()
                        .userId(userId)
                        .title(defaultTitle(listType))
                        .listType(listType)
                        .isPublic(false)
                        .isPinned(false)
                        .build()));
    }

    private String defaultTitle(ListType listType) {
        return switch (listType) {
            case WATCHLIST -> "İzleme Listesi";
            case READLIST -> "Okuma Listesi";
            case CUSTOM -> throw new IllegalArgumentException("CUSTOM bir sistem listesi değildir");
        };
    }
}
