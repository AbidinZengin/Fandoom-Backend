package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.common.util.SlugGenerator;
import com.example.fandoom_backend.community.dto.AuthorSummary;
import com.example.fandoom_backend.community.dto.ThreadDetailResponse;
import com.example.fandoom_backend.community.dto.ThreadPatchRequest;
import com.example.fandoom_backend.community.dto.ThreadRequest;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.entity.ThreadTag;
import com.example.fandoom_backend.community.mapper.ThreadMapper;
import com.example.fandoom_backend.community.repository.ThreadBookmarkRepository;
import com.example.fandoom_backend.community.repository.ThreadLikeRepository;
import com.example.fandoom_backend.community.repository.ThreadRepository;
import com.example.fandoom_backend.community.repository.ThreadTagRepository;
import com.example.fandoom_backend.community.specification.ThreadSpecificationBuilder;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.service.SeriesService;
import com.example.fandoom_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThreadServiceImpl implements ThreadService {

    private static final int MAX_TAGS_PER_THREAD = 10;
    private static final int MAX_TAG_LENGTH = 50;

    private final ThreadRepository threadRepository;
    private final ThreadTagRepository threadTagRepository;
    private final ThreadLikeRepository threadLikeRepository;
    private final ThreadBookmarkRepository threadBookmarkRepository;
    private final ThreadMapper threadMapper;
    private final MovieService movieService;
    private final SeriesService seriesService;
    private final UserService userService;
    private final UserProfileService userProfileService;

    @Override
    public PageResponse<ThreadSummaryResponse> list(
            ThreadSurface surface, String productionSlug, List<String> tags, String sort, Long viewerId,
            Pageable pageable) {
        List<Long> tagThreadIds = null;
        if (tags != null && !tags.isEmpty()) {
            List<String> normalizedTags = tags.stream()
                    .map(SlugGenerator::slugify)
                    .filter(t -> !t.isBlank())
                    .distinct()
                    .toList();
            tagThreadIds = threadTagRepository.findByTagIn(normalizedTags).stream()
                    .map(tt -> tt.getThread().getId())
                    .distinct()
                    .toList();
            if (tagThreadIds.isEmpty()) {
                return PageResponse.from(Page.empty(pageable));
            }
        }

        Specification<Thread> specification = Specification.allOf(
                Stream.of(
                                ThreadSpecificationBuilder.isPublished(),
                                ThreadSpecificationBuilder.hasSurface(surface),
                                ThreadSpecificationBuilder.hasProductionSlug(productionSlug),
                                ThreadSpecificationBuilder.hasIdIn(tagThreadIds))
                        .filter(Objects::nonNull)
                        .toList());

        Pageable pageableWithSort =
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), resolveSort(sort));
        Page<Thread> page = threadRepository.findAll(specification, pageableWithSort);
        return PageResponse.from(mapSummaryPage(page, viewerId));
    }

    @Override
    @Transactional
    public ThreadDetailResponse getBySlug(String slug, Long viewerId) {
        Thread thread = threadRepository.findBySlugAndStatus(slug, ThreadStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Thread bulunamadı: slug=" + slug));
        AuthorSummary author = authorOf(thread.getAuthorId());
        boolean liked = isLikedBy(viewerId, thread.getId());
        boolean bookmarked = isBookmarkedBy(viewerId, thread.getId());
        return threadMapper.toDetailResponse(thread, tagsOf(thread.getId()), author, liked, bookmarked);
    }

    @Override
    @Transactional
    public ThreadDetailResponse create(Long authorId, ThreadRequest request) {
        validateProductionSlug(request.productionSlug());
        Thread thread = Thread.builder()
                .slug(SlugGenerator.generateUnique(request.title(), threadRepository::existsBySlug))
                .surface(request.surface())
                .title(request.title())
                .body(request.body())
                .imageUrl(request.imageUrl())
                .spoilerFlagged(request.spoilerFlagged())
                .status(ThreadStatus.PUBLISHED)
                .authorId(authorId)
                .productionSlug(request.productionSlug())
                .build();
        thread = threadRepository.save(thread);
        List<String> tags = applyTags(thread, request.tags() == null ? List.of() : request.tags());
        AuthorSummary author = authorOf(authorId);
        return threadMapper.toDetailResponse(thread, tags, author, false, false);
    }

    @Override
    @Transactional
    public ThreadDetailResponse update(Long userId, boolean moderator, String slug, ThreadPatchRequest request) {
        Thread thread = findEditableBySlug(slug);
        assertOwnerOrModerator(thread.getAuthorId(), userId, moderator);
        if (request.title() != null && !request.title().equals(thread.getTitle())) {
            thread.setSlug(SlugGenerator.generateUnique(request.title(),
                    candidate -> threadRepository.existsBySlugAndIdNot(candidate, thread.getId())));
            thread.setTitle(request.title());
        }
        if (request.body() != null) {
            thread.setBody(request.body());
        }
        if (request.imageUrl() != null) {
            thread.setImageUrl(request.imageUrl());
        }
        if (request.spoilerFlagged() != null) {
            thread.setSpoilerFlagged(request.spoilerFlagged());
        }
        List<String> tags = applyTags(thread, request.tags());
        AuthorSummary author = authorOf(thread.getAuthorId());
        boolean liked = isLikedBy(userId, thread.getId());
        boolean bookmarked = isBookmarkedBy(userId, thread.getId());
        return threadMapper.toDetailResponse(thread, tags, author, liked, bookmarked);
    }

    @Override
    @Transactional
    public void delete(Long userId, boolean moderator, String slug) {
        Thread thread = findEditableBySlug(slug);
        assertOwnerOrModerator(thread.getAuthorId(), userId, moderator);
        thread.setStatus(ThreadStatus.DELETED);
    }

    private Thread findEditableBySlug(String slug) {
        return threadRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Thread bulunamadı: slug=" + slug));
    }

    private void assertOwnerOrModerator(Long authorId, Long userId, boolean moderator) {
        if (moderator) {
            return;
        }
        if (!authorId.equals(userId)) {
            throw new AccessDeniedException("Bu thread'i düzenleme/silme yetkiniz yok");
        }
    }

    private void validateProductionSlug(String productionSlug) {
        if (productionSlug == null) {
            return;
        }
        if (!movieService.existsBySlug(productionSlug) && !seriesService.existsBySlug(productionSlug)) {
            throw new InvalidReferenceException("Geçersiz productionSlug: " + productionSlug);
        }
    }

    // rawTags null ise (PATCH'te alan gönderilmediyse) mevcut tag'ler KORUNUR —
    // yalnızca explicit boş liste ([]) gönderilirse tüm tag'ler silinir
    // (BlogServiceImpl.applyTags/applyBlocks deseniyle tutarlı).
    private List<String> applyTags(Thread thread, List<String> rawTags) {
        if (rawTags == null) {
            return tagsOf(thread.getId());
        }
        if (rawTags.size() > MAX_TAGS_PER_THREAD) {
            throw new InvalidReferenceException("Bir thread'e en fazla " + MAX_TAGS_PER_THREAD + " tag eklenebilir");
        }
        List<String> normalized = rawTags.stream()
                .map(SlugGenerator::slugify)
                .filter(t -> !t.isBlank())
                .distinct()
                .toList();
        for (String tag : normalized) {
            if (tag.length() > MAX_TAG_LENGTH) {
                throw new InvalidReferenceException("Tag en fazla " + MAX_TAG_LENGTH + " karakter olabilir: " + tag);
            }
        }
        threadTagRepository.deleteByThread_Id(thread.getId());
        for (String tag : normalized) {
            threadTagRepository.save(ThreadTag.builder().thread(thread).tag(tag).build());
        }
        return normalized;
    }

    private List<String> tagsOf(Long threadId) {
        return threadTagRepository.findByThread_Id(threadId).stream().map(ThreadTag::getTag).toList();
    }

    private Sort resolveSort(String sort) {
        return switch (sort == null ? "hot" : sort) {
            case "new" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "top" -> Sort.by(Sort.Direction.DESC, "likeCount");
            default -> Sort.by(Sort.Direction.DESC, "hotScore");
        };
    }

    private AuthorSummary authorOf(Long authorId) {
        if (authorId == null) {
            return null;
        }
        String username = userService.getUsernamesByIds(Set.of(authorId)).get(authorId);
        String avatarUrl = userProfileService.getAvatarUrlsByUserIds(Set.of(authorId)).get(authorId);
        return new AuthorSummary(authorId, username, avatarUrl);
    }

    private boolean isLikedBy(Long viewerId, Long threadId) {
        return viewerId != null && threadLikeRepository.existsByUserIdAndThreadId(viewerId, threadId);
    }

    private boolean isBookmarkedBy(Long viewerId, Long threadId) {
        return viewerId != null && threadBookmarkRepository.existsByUserIdAndThreadId(viewerId, threadId);
    }

    private Page<ThreadSummaryResponse> mapSummaryPage(Page<Thread> page, Long viewerId) {
        List<Long> ids = page.getContent().stream().map(Thread::getId).toList();
        Map<Long, List<String>> tagsByThread = threadTagRepository.findByThread_IdIn(ids).stream()
                .collect(Collectors.groupingBy(tt -> tt.getThread().getId(),
                        Collectors.mapping(ThreadTag::getTag, Collectors.toList())));

        Set<Long> authorIds = page.getContent().stream().map(Thread::getAuthorId).collect(Collectors.toSet());
        Map<Long, String> usernames = userService.getUsernamesByIds(authorIds);
        Map<Long, String> avatarUrls = userProfileService.getAvatarUrlsByUserIds(authorIds);

        Set<Long> likedThreadIds = (viewerId == null || ids.isEmpty())
                ? Set.of()
                : threadLikeRepository.findThreadIdsByUserIdAndThreadIdIn(viewerId, ids);
        Set<Long> bookmarkedThreadIds = (viewerId == null || ids.isEmpty())
                ? Set.of()
                : threadBookmarkRepository.findThreadIdsByUserIdAndThreadIdIn(viewerId, ids);

        return page.map(thread -> threadMapper.toSummaryResponse(
                thread,
                tagsByThread.getOrDefault(thread.getId(), List.of()),
                new AuthorSummary(thread.getAuthorId(), usernames.get(thread.getAuthorId()),
                        avatarUrls.get(thread.getAuthorId())),
                likedThreadIds.contains(thread.getId()),
                bookmarkedThreadIds.contains(thread.getId())));
    }
}
