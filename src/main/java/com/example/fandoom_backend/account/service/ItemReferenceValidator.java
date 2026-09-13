package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.blog.service.BlogService;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.service.SeriesService;
import com.example.fandoom_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// UserSavedItemServiceImpl/UserLikeServiceImpl/UserFollowServiceImpl arasında
// paylaşılan cross-module doğrulama — Cast'in subjectType+subjectId
// deseninin (person/service/CastServiceImpl) birebir tekrarı. account/
// dışına sızmayan (paket-private) bir yardımcı, ayrı bir public interface
// açmaya gerek yok — sadece bu üç servis içi kullanım.
@Component
@RequiredArgsConstructor
class ItemReferenceValidator {

    private final MovieService movieService;
    private final SeriesService seriesService;
    private final BlogService blogService;
    // UserService: user/ stabil bir modül (community/'ye bağımlı değil), USER
    // kişi takibi için eklendi — döngüsel bağımlılık riski taşımaz.
    private final UserService userService;

    void assertExists(SavedItemType itemType, Long itemId) {
        boolean exists = switch (itemType) {
            case MOVIE -> movieService.existsById(itemId);
            case SERIES -> seriesService.existsById(itemId);
            case BLOG -> blogService.existsById(itemId);
            case USER -> userService.existsById(itemId);
        };
        if (!exists) {
            throw new InvalidReferenceException("Geçersiz " + itemType + " id: " + itemId);
        }
    }
}
