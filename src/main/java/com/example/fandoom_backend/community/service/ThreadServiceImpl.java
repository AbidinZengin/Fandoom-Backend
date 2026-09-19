package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.KeysetPageResponse;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.common.specification.KeysetSpecification;
import com.example.fandoom_backend.common.util.KeysetCursor;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
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

    // Cache yalnızca anonim (viewerId == null) ve ilk 5 sayfa için: giriş yapmış kullanıcıya özel
    // isLiked/isBookmarked alanları paylaşılan cache'e girmemeli, derin sayfalar nadir + sınırsız çeşitli.
    @Override
    @Cacheable(cacheNames = ThreadCacheNames.LIST, sync = true,
            condition = "#viewerId == null && #pageable.pageNumber < 5",
            key = "T(com.example.fandoom_backend.community.service.ThreadCacheKeys).list(#surface, #productionSlug, #tags, #sort, #pageable)")
    public PageResponse<ThreadSummaryResponse> list(
            ThreadSurface surface, String productionSlug, List<String> tags, String sort, Long viewerId,
            Pageable pageable) {
        List<Long> tagThreadIds = resolveTagThreadIds(tags);
        if (tagThreadIds != null && tagThreadIds.isEmpty()) {
            return PageResponse.from(Page.empty(pageable));
        }
        Specification<Thread> specification = buildSpecification(surface, productionSlug, tagThreadIds);
        Pageable pageableWithSort =
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), resolveSort(sort));
        Page<Thread> page = threadRepository.findAll(specification, pageableWithSort);
        return PageResponse.from(new PageImpl<>(
                mapSummaries(page.getContent(), viewerId), page.getPageable(), page.getTotalElements()));
    }

    // Keyset (cursor) sayfalama: OFFSET/COUNT yok, "size+1" satır çekilip hasNext türetilir.
    // Sıralama (sortField DESC, id DESC) — id tie-breaker olduğu için cursor tekildir.
    // Yalnızca anonim istekte ve İLK sayfa (cursor == null) cache'lenir.
    @Override
    @Cacheable(cacheNames = ThreadCacheNames.LIST, sync = true,
            condition = "#viewerId == null && #cursor == null",
            key = "T(com.example.fandoom_backend.community.service.ThreadCacheKeys).firstCursorPage(#surface, #productionSlug, #tags, #sort, #size)")
    public KeysetPageResponse<ThreadSummaryResponse> listByCursor(
            ThreadSurface surface, String productionSlug, List<String> tags, String sort, Long viewerId,
            String cursor, int size) {
        List<Long> tagThreadIds = resolveTagThreadIds(tags);
        if (tagThreadIds != null && tagThreadIds.isEmpty()) {
            return new KeysetPageResponse<>(List.of(), false, null);
        }
        String sortField = sortField(sort);
        Specification<Thread> specification = buildSpecification(surface, productionSlug, tagThreadIds);
        KeysetCursor decoded = KeysetCursor.decode(cursor);
        if (decoded != null) {
            specification = specification.and(afterCursor(sortField, decoded));
        }
        Sort order = Sort.by(Sort.Direction.DESC, sortField).and(Sort.by(Sort.Direction.DESC, "id"));
        List<Thread> rows = threadRepository.findBy(specification,
                q -> q.sortBy(order).limit(size + 1).all());

        boolean hasNext = rows.size() > size;
        List<Thread> pageRows = hasNext ? rows.subList(0, size) : rows;
        String nextCursor = hasNext ? cursorOf(sortField, pageRows.get(pageRows.size() - 1)) : null;
        return new KeysetPageResponse<>(mapSummaries(pageRows, viewerId), hasNext, nextCursor);
    }

    // null → tag filtresi yok; boş liste → filtre var ama eşleşen thread yok.
    private List<Long> resolveTagThreadIds(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        List<String> normalizedTags = tags.stream()
                .map(SlugGenerator::slugify)
                .filter(t -> !t.isBlank())
                .distinct()
                .toList();
        return threadTagRepository.findByTagIn(normalizedTags).stream()
                .map(tt -> tt.getThread().getId())
                .distinct()
                .toList();
    }

    private Specification<Thread> buildSpecification(
            ThreadSurface surface, String productionSlug, List<Long> tagThreadIds) {
        return Specification.allOf(
                Stream.of(
                                ThreadSpecificationBuilder.isPublished(),
                                ThreadSpecificationBuilder.hasSurface(surface),
                                ThreadSpecificationBuilder.hasProductionSlug(productionSlug),
                                ThreadSpecificationBuilder.hasIdIn(tagThreadIds))
                        .filter(Objects::nonNull)
                        .toList());
    }

    private Specification<Thread> afterCursor(String sortField, KeysetCursor cursor) {
        try {
            return switch (sortField) {
                case "createdAt" -> KeysetSpecification.<Thread, LocalDateTime>after(
                        sortField, LocalDateTime.parse(cursor.value()), cursor.id());
                case "likeCount" -> KeysetSpecification.<Thread, Integer>after(
                        sortField, Integer.valueOf(cursor.value()), cursor.id());
                default -> KeysetSpecification.<Thread, Double>after(
                        sortField, Double.valueOf(cursor.value()), cursor.id());
            };
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new InvalidReferenceException("Geçersiz cursor");
        }
    }

    private String cursorOf(String sortField, Thread last) {
        String value = switch (sortField) {
            case "createdAt" -> last.getCreatedAt().toString();
            case "likeCount" -> String.valueOf(last.getLikeCount());
            default -> String.valueOf(last.getHotScore());
        };
        return new KeysetCursor(value, last.getId()).encode();
    }

    @Override
    @Transactional
    @Cacheable(cacheNames = ThreadCacheNames.DETAIL, sync = true, condition = "#viewerId == null",
            key = "'slug:' + #slug")
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
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
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
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
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
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
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
        return Sort.by(Sort.Direction.DESC, sortField(sort));
    }

    private String sortField(String sort) {
        return switch (sort == null ? "hot" : sort) {
            case "new" -> "createdAt";
            case "top" -> "likeCount";
            default -> "hotScore";
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

    private List<ThreadSummaryResponse> mapSummaries(List<Thread> threads, Long viewerId) {
        List<Long> ids = threads.stream().map(Thread::getId).toList();
        Map<Long, List<String>> tagsByThread = threadTagRepository.findByThread_IdIn(ids).stream()
                .collect(Collectors.groupingBy(tt -> tt.getThread().getId(),
                        Collectors.mapping(ThreadTag::getTag, Collectors.toList())));

        Set<Long> authorIds = threads.stream().map(Thread::getAuthorId).collect(Collectors.toSet());
        Map<Long, String> usernames = userService.getUsernamesByIds(authorIds);
        Map<Long, String> avatarUrls = userProfileService.getAvatarUrlsByUserIds(authorIds);

        Set<Long> likedThreadIds = (viewerId == null || ids.isEmpty())
                ? Set.of()
                : threadLikeRepository.findThreadIdsByUserIdAndThreadIdIn(viewerId, ids);
        Set<Long> bookmarkedThreadIds = (viewerId == null || ids.isEmpty())
                ? Set.of()
                : threadBookmarkRepository.findThreadIdsByUserIdAndThreadIdIn(viewerId, ids);

        return threads.stream()
                .map(thread -> threadMapper.toSummaryResponse(
                        thread,
                        tagsByThread.getOrDefault(thread.getId(), List.of()),
                        new AuthorSummary(thread.getAuthorId(), usernames.get(thread.getAuthorId()),
                                avatarUrls.get(thread.getAuthorId())),
                        likedThreadIds.contains(thread.getId()),
                        bookmarkedThreadIds.contains(thread.getId())))
                .toList();
    }
}
