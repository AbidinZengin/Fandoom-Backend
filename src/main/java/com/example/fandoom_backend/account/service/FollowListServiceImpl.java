package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.FollowUserResponse;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.ProfileCard;
import com.example.fandoom_backend.community.service.UserProfileService;
import com.example.fandoom_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowListServiceImpl implements FollowListService {

    private static final int BIO_PREVIEW_LENGTH = 160;

    private final ProfileFollowService profileFollowService;
    private final UserService userService;
    private final UserProfileService userProfileService;

    @Override
    public PageResponse<FollowUserResponse> followers(String username, Long viewerId, Pageable pageable) {
        Long userId = userService.getIdByUsername(username);
        if (profileFollowService.isRestricted(viewerId, userId)) {
            return emptyPage(pageable);
        }
        return enrich(profileFollowService.listFollowerIds(userId, pageable), viewerId);
    }

    @Override
    public PageResponse<FollowUserResponse> following(String username, Long viewerId, Pageable pageable) {
        Long userId = userService.getIdByUsername(username);
        if (profileFollowService.isRestricted(viewerId, userId)) {
            return emptyPage(pageable);
        }
        return enrich(profileFollowService.listFollowingIds(userId, pageable), viewerId);
    }

    @Override
    public PageResponse<FollowUserResponse> pendingRequests(Long userId, Pageable pageable) {
        return enrich(profileFollowService.listRequesterIds(userId, pageable), userId);
    }

    @Override
    public PageResponse<FollowUserResponse> users(String query, Long viewerId, Pageable pageable) {
        return enrich(userService.searchPublicIds(query, pageable), viewerId);
    }

    // Kısıtlı profilde 403 değil boş sayfa: FE kilidi restricted bayrağından çizer.
    private PageResponse<FollowUserResponse> emptyPage(Pageable pageable) {
        return new PageResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0, true);
    }

    // Sayfa başına: username (1 sorgu), profil kartı (1 sorgu), following bayrağı (1 sorgu) — N+1 yok.
    private PageResponse<FollowUserResponse> enrich(PageResponse<Long> page, Long viewerId) {
        Set<Long> ids = new HashSet<>(page.content());
        Map<Long, String> usernames = userService.getUsernamesByIds(ids);
        Map<Long, ProfileCard> cards = userProfileService.getCardsByUserIds(ids);
        Set<Long> followedByViewer = profileFollowService.filterFollowedBy(viewerId, ids);

        List<FollowUserResponse> content = page.content().stream()
                // Silinmiş kullanıcı (username çözülemez) listede gösterilmez.
                .filter(usernames::containsKey)
                .map(id -> {
                    ProfileCard card = cards.get(id);
                    // Gizli profilin bio/accentColor'ı yalnız sahibine ve onaylı takipçilerine görünür;
                    // avatar vitrindir, her zaman görünür (bkz. ProfileCard).
                    boolean contentVisible = card == null || !card.isPrivate()
                            || id.equals(viewerId) || followedByViewer.contains(id);
                    return new FollowUserResponse(
                            usernames.get(id),
                            card == null ? null : card.avatarUrl(),
                            card == null || !contentVisible ? null : preview(card.bio()),
                            card == null || !contentVisible ? null : card.accentColor(),
                            followedByViewer.contains(id));
                })
                .toList();
        return new PageResponse<>(content, page.page(), page.size(), page.totalElements(),
                page.totalPages(), page.last());
    }

    private String preview(String bio) {
        if (bio == null || bio.length() <= BIO_PREVIEW_LENGTH) {
            return bio;
        }
        return bio.substring(0, BIO_PREVIEW_LENGTH);
    }
}
