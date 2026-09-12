package com.example.fandoom_backend.blog.service;

import com.example.fandoom_backend.blog.dto.BlogBlockRequest;
import com.example.fandoom_backend.blog.dto.BlogDetailResponse;
import com.example.fandoom_backend.blog.dto.BlogRequest;
import com.example.fandoom_backend.blog.dto.BlogSummaryResponse;
import com.example.fandoom_backend.blog.dto.BlogTagRequest;
import com.example.fandoom_backend.blog.entity.Blog;
import com.example.fandoom_backend.blog.entity.BlogBlock;
import com.example.fandoom_backend.blog.entity.BlogRelation;
import com.example.fandoom_backend.blog.entity.BlogStatus;
import com.example.fandoom_backend.blog.entity.BlogTag;
import com.example.fandoom_backend.blog.entity.SubjectType;
import com.example.fandoom_backend.blog.mapper.BlogMapper;
import com.example.fandoom_backend.blog.repository.BlogRelationRepository;
import com.example.fandoom_backend.blog.repository.BlogRepository;
import com.example.fandoom_backend.blog.repository.BlogTagRepository;
import com.example.fandoom_backend.account.entity.ActivityType;
import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.account.service.ActivityLogService;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.common.exception.TranslationIncompleteException;
import com.example.fandoom_backend.common.util.LocalizedTextResolver;
import com.example.fandoom_backend.common.util.SlugGenerator;
import com.example.fandoom_backend.franchise.service.FranchiseService;
import com.example.fandoom_backend.media.service.ImageStorageService;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.service.SeriesService;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlogServiceImpl implements BlogService {

    // Bir blog sayfasının altında gösterilecek sabit vitrin boyutu
    // (kürasyon + tag merdiveni + recency/popülerlik ile doldurulur).
    private static final int RELATED_SHOWCASE_SIZE = 6;
    private static final int WORDS_PER_MINUTE = 200;

    private final BlogRepository blogRepository;
    private final BlogTagRepository blogTagRepository;
    private final BlogRelationRepository blogRelationRepository;
    private final BlogMapper blogMapper;
    private final MovieService movieService;
    private final SeriesService seriesService;
    private final FranchiseService franchiseService;
    private final ImageStorageService imageStorageService;
    private final ActivityLogService activityLogService;

    @Override
    public PageResponse<BlogSummaryResponse> list(Pageable pageable) {
        Page<BlogSummaryResponse> page = blogRepository.findAll(pageable)
                .map(blogMapper::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional
    public BlogDetailResponse getById(Long id) {
        Blog blog = findEntityById(id);
        logReadIfAuthenticated(blog.getId());
        return blogMapper.toDetailResponse(blog, resolveRelated(blog));
    }

    @Override
    @Transactional
    public BlogDetailResponse getBySlug(String slug) {
        Blog blog = blogRepository.findBySlugAndStatus(slug, BlogStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Blog bulunamadı: slug=" + slug));
        blogRepository.incrementViewCount(blog.getId());
        blog.setViewCount(blog.getViewCount() + 1);
        logReadIfAuthenticated(blog.getId());
        return blogMapper.toDetailResponse(blog, resolveRelated(blog));
    }

    // GET /api/blogs/{id} ve /api/blogs/slug/{slug} tamamen permitAll, ama
    // JwtAuthenticationFilter geçerli bir Bearer token varsa
    // SecurityContextHolder'ı endpoint'in auth gerektirip gerektirmediğine
    // bakmaksızın HER istekte doldurur — bu yüzden burada sadece kontrol
    // edip login olmayan ziyaretçi için sessizce atlıyoruz (yeni bir filtre
    // değişikliği gerekmiyor).
    private void logReadIfAuthenticated(Long blogId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails principal) {
            activityLogService.record(principal.getId(), ActivityType.READ_BLOG, blogId, SavedItemType.BLOG);
        }
    }

    @Override
    public List<BlogSummaryResponse> findRelatedForProduction(
            SubjectType productionType, String productionSlug,
            Integer seasonNumber, Integer episodeNumber, int limit) {
        Long subjectId = resolveProductionId(productionType, productionSlug);
        Long franchiseId = resolveFranchiseId(productionType, subjectId);

        List<Blog> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        collectSpecificMatches(result, seen, productionType, subjectId, franchiseId, seasonNumber, episodeNumber, limit);
        fillWithPopular(result, seen, limit);
        return blogMapper.toSummaryResponseList(result);
    }

    @Override
    @Transactional
    public BlogDetailResponse create(BlogRequest request) {
        if (request.status() == BlogStatus.PUBLISHED) {
            validateMirror(request);
        }
        Blog blog = Blog.builder()
                .titleTr(request.titleTr())
                .title(request.title())
                .slug(SlugGenerator.generateUnique(request.title(), blogRepository::existsBySlug))
                .kickerTr(request.kickerTr())
                .kicker(request.kicker())
                .axisTr(request.axisTr())
                .axis(request.axis())
                .imageUrl(request.imageUrl())
                .imageUrlLarge(request.imageUrlLarge())
                .imageAltTr(request.imageAltTr())
                .imageAlt(request.imageAlt())
                .spoilerThroughSeasonNumber(request.spoilerThroughSeasonNumber())
                .spoilerThroughEpisodeNumber(request.spoilerThroughEpisodeNumber())
                .recommendedRank(request.recommendedRank())
                .spoilerFree(request.spoilerFree())
                .status(request.status())
                .format(request.format())
                .publishedAt(resolvePublishedAt(request, null))
                .build();
        applyBlocks(blog, request.blocks());
        applyTags(blog, request.tags());
        blog = blogRepository.save(blog);
        return blogMapper.toDetailResponse(blog, resolveRelated(blog));
    }

    @Override
    @Transactional
    public BlogDetailResponse update(Long id, BlogRequest request) {
        if (request.status() == BlogStatus.PUBLISHED) {
            validateMirror(request);
        }
        Blog blog = findEntityById(id);
        imageStorageService.deleteIfChanged(blog.getImageUrl(), request.imageUrl());
        imageStorageService.deleteIfChanged(blog.getImageUrlLarge(), request.imageUrlLarge());
        if (!blog.getTitle().equals(request.title())) {
            blog.setSlug(SlugGenerator.generateUnique(request.title(),
                    slug -> blogRepository.existsBySlugAndIdNot(slug, id)));
        }
        blog.setTitleTr(request.titleTr());
        blog.setTitle(request.title());
        blog.setKickerTr(request.kickerTr());
        blog.setKicker(request.kicker());
        blog.setAxisTr(request.axisTr());
        blog.setAxis(request.axis());
        blog.setImageUrl(request.imageUrl());
        blog.setImageUrlLarge(request.imageUrlLarge());
        blog.setImageAltTr(request.imageAltTr());
        blog.setImageAlt(request.imageAlt());
        blog.setSpoilerThroughSeasonNumber(request.spoilerThroughSeasonNumber());
        blog.setSpoilerThroughEpisodeNumber(request.spoilerThroughEpisodeNumber());
        blog.setRecommendedRank(request.recommendedRank());
        blog.setSpoilerFree(request.spoilerFree());
        blog.setFormat(request.format());
        blog.setPublishedAt(resolvePublishedAt(request, blog));
        blog.setStatus(request.status());
        applyBlocks(blog, request.blocks());
        applyTags(blog, request.tags());
        return blogMapper.toDetailResponse(blog, resolveRelated(blog));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Blog blog = findEntityById(id);
        imageStorageService.delete(blog.getImageUrl());
        imageStorageService.delete(blog.getImageUrlLarge());
        blogRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return blogRepository.existsById(id);
    }

    @Override
    @Transactional
    public List<BlogSummaryResponse> replaceRelated(Long id, List<Long> relatedBlogIds) {
        Blog source = findEntityById(id);
        if (relatedBlogIds.contains(id)) {
            throw new InvalidReferenceException("Bir blog kendisiyle ilişkilendirilemez: id=" + id);
        }
        List<Blog> targets = relatedBlogIds.stream().distinct().map(this::findEntityById).toList();

        source.clearOutgoingRelations();
        int orderIndex = 0;
        for (Blog target : targets) {
            source.addOutgoingRelation(BlogRelation.builder()
                    .relatedBlog(target)
                    .orderIndex(orderIndex++)
                    .build());
        }
        return blogMapper.toSummaryResponseList(targets);
    }

    // ---- kademeli-geri-düşüş merdiveni ----

    private void collectSpecificMatches(List<Blog> result, Set<Long> seen,
                                         SubjectType subjectType, Long subjectId, Long franchiseId,
                                         Integer seasonNumber, Integer episodeNumber, int limit) {
        if (subjectType != null && subjectId != null) {
            if (episodeNumber != null) {
                collect(result, seen, blogTagRepository
                        .findByExactEpisode(subjectType, subjectId, seasonNumber, episodeNumber), limit);
            }
            if (result.size() < limit && seasonNumber != null) {
                collect(result, seen, blogTagRepository
                        .findBySameSeason(subjectType, subjectId, seasonNumber), limit);
            }
            if (result.size() < limit) {
                collect(result, seen, blogTagRepository
                        .findBySameProduction(subjectType, subjectId), limit);
            }
        }
        if (result.size() < limit && franchiseId != null) {
            collect(result, seen, blogTagRepository.findBySameFranchise(franchiseId), limit);
        }
    }

    private void fillWithPopular(List<Blog> result, Set<Long> seen, int limit) {
        if (result.size() >= limit) {
            return;
        }
        int remaining = limit - result.size();
        List<Blog> popular = seen.isEmpty()
                ? blogRepository.findByStatusOrderByPublishedAtDescViewCountDesc(
                        BlogStatus.PUBLISHED, PageRequest.of(0, remaining))
                : blogRepository.findByStatusAndIdNotInOrderByPublishedAtDescViewCountDesc(
                        BlogStatus.PUBLISHED, seen, PageRequest.of(0, remaining));
        collect(result, seen, popular, limit);
    }

    private void collect(List<Blog> result, Set<Long> seen, List<Blog> candidates, int limit) {
        for (Blog candidate : candidates) {
            if (result.size() >= limit) {
                return;
            }
            if (seen.add(candidate.getId())) {
                result.add(candidate);
            }
        }
    }

    // Blog sayfasının kendi altındaki "ilgili içerikler": önce editörün
    // BlogRelation ile elle seçtiği liste, boşsa/eksikse bu içeriğin KENDİ
    // tag'lerinden yürütülen merdivenle, o da yetmezse recency+popülerlik ile
    // tamamlanır.
    private List<BlogSummaryResponse> resolveRelated(Blog blog) {
        Set<Long> seen = new HashSet<>();
        seen.add(blog.getId());
        List<Blog> result = new ArrayList<>();

        for (BlogRelation relation : blogRelationRepository
                .findBySourceBlogIdOrderByOrderIndexAsc(blog.getId())) {
            if (result.size() >= RELATED_SHOWCASE_SIZE) {
                break;
            }
            Blog related = relation.getRelatedBlog();
            if (related.getStatus() == BlogStatus.PUBLISHED && seen.add(related.getId())) {
                result.add(related);
            }
        }

        if (result.size() < RELATED_SHOWCASE_SIZE) {
            for (BlogTag tag : blog.getTags()) {
                if (result.size() >= RELATED_SHOWCASE_SIZE) {
                    break;
                }
                collectSpecificMatches(result, seen, tag.getSubjectType(), tag.getSubjectId(), tag.getFranchiseId(),
                        tag.getSeasonNumber(), tag.getEpisodeNumber(), RELATED_SHOWCASE_SIZE);
            }
        }

        fillWithPopular(result, seen, RELATED_SHOWCASE_SIZE);
        return blogMapper.toSummaryResponseList(result);
    }

    // ---- yardımcılar ----

    // requests null ise (generic scalar-only PUT gibi) mevcut bloklar KORUNUR —
    // sadece explicit bos liste ([]) gonderilirse tum bloklar silinir.
    private void applyBlocks(Blog blog, List<BlogBlockRequest> requests) {
        if (requests == null) {
            return;
        }
        blog.clearBlocks();
        int orderIndex = 0;
        int wordCount = 0;
        for (BlogBlockRequest request : requests) {
            BlogBlock block = BlogBlock.builder()
                    .blockType(request.blockType())
                    .sceneKey(request.sceneKey())
                    .contentTr(request.contentTr())
                    .content(request.content())
                    .imageUrl(request.imageUrl())
                    .imageAltTr(request.imageAltTr())
                    .imageAlt(request.imageAlt())
                    .build();
            block.setOrderIndex(orderIndex++);
            blog.addBlock(block);
            String content = LocalizedTextResolver.resolve(request.contentTr(), request.content());
            if (content != null && !content.isBlank()) {
                wordCount += content.trim().split("\\s+").length;
            }
        }
        blog.setReadingTimeMinutes(
                wordCount == 0 ? null : Math.max(1, Math.round(wordCount / (float) WORDS_PER_MINUTE)));
    }

    // K1 "tam ayna" kuralı: PUBLISHED'e geçerken bir dilde dolu olan her
    // çevrilebilir alan diğer dilde de dolu olmalı. title zaten @NotBlank
    // olduğundan bu kontrol pratikte titleTr'yi de zorunlu kılar.
    private void validateMirror(BlogRequest request) {
        List<String> errors = new ArrayList<>();
        checkMirror(errors, "titleTr/title", request.titleTr(), request.title());
        checkMirror(errors, "kickerTr/kicker", request.kickerTr(), request.kicker());
        checkMirror(errors, "axisTr/axis", request.axisTr(), request.axis());
        checkMirror(errors, "imageAltTr/imageAlt", request.imageAltTr(), request.imageAlt());
        if (request.blocks() != null) {
            int index = 0;
            for (BlogBlockRequest block : request.blocks()) {
                checkMirror(errors, "blocks[" + index + "].contentTr/content", block.contentTr(), block.content());
                checkMirror(errors, "blocks[" + index + "].imageAltTr/imageAlt", block.imageAltTr(), block.imageAlt());
                index++;
            }
        }
        if (!errors.isEmpty()) {
            throw new TranslationIncompleteException(errors);
        }
    }

    private void checkMirror(List<String> errors, String fieldPair, String trValue, String defaultValue) {
        boolean trBlank = trValue == null || trValue.isBlank();
        boolean defaultBlank = defaultValue == null || defaultValue.isBlank();
        if (trBlank != defaultBlank) {
            errors.add(fieldPair + ": iki dilde de dolu olmalı (eksik: " + (trBlank ? "tr" : "en") + ")");
        }
    }

    // requests null ise (generic scalar-only PUT gibi) mevcut tag'ler KORUNUR —
    // sadece explicit bos liste ([]) gonderilirse tum tag'ler silinir.
    private void applyTags(Blog blog, List<BlogTagRequest> requests) {
        if (requests == null) {
            return;
        }
        blog.clearTags();
        for (BlogTagRequest request : requests) {
            boolean hasSubject = request.subjectType() != null && request.subjectId() != null;
            boolean hasFranchise = request.franchiseId() != null;
            int variantCount = (hasSubject ? 1 : 0) + (hasFranchise ? 1 : 0);
            if (variantCount != 1) {
                throw new InvalidReferenceException(
                        "Tag ya subjectType+subjectId ya da yalnızca franchiseId taşımalı: " + request);
            }
            Long resolvedFranchiseId = request.franchiseId();
            if (hasSubject) {
                boolean exists = switch (request.subjectType()) {
                    case MOVIE -> movieService.existsById(request.subjectId());
                    case SERIES -> seriesService.existsById(request.subjectId());
                };
                if (!exists) {
                    throw new InvalidReferenceException(
                            "Geçersiz " + request.subjectType() + " id: " + request.subjectId());
                }
                // Client franchiseId göndermez: subject'in ait olduğu production'ın
                // franchise'ı burada otomatik hesaplanıp aynı BlogTag satırına
                // yazılır — findBySameFranchise'ın subject-etiketli blogları da
                // TEK sorguyla yakalayabilmesi için (bkz. BlogTag yorumu).
                resolvedFranchiseId = resolveFranchiseId(request.subjectType(), request.subjectId());
            } else if (hasFranchise && !franchiseService.existsById(request.franchiseId())) {
                throw new InvalidReferenceException("Geçersiz franchise id: " + request.franchiseId());
            }
            blog.addTag(BlogTag.builder()
                    .subjectType(request.subjectType())
                    .subjectId(request.subjectId())
                    .seasonNumber(request.seasonNumber())
                    .episodeNumber(request.episodeNumber())
                    .franchiseId(resolvedFranchiseId)
                    .build());
        }
    }

    private Long resolveProductionId(SubjectType productionType, String productionSlug) {
        return switch (productionType) {
            case MOVIE -> movieService.getBySlug(productionSlug).id();
            case SERIES -> seriesService.getBySlug(productionSlug).id();
        };
    }

    private Long resolveFranchiseId(SubjectType productionType, Long subjectId) {
        return switch (productionType) {
            case MOVIE -> movieService.getById(subjectId).franchiseId();
            case SERIES -> seriesService.getById(subjectId).franchiseId();
        };
    }

    private Blog findEntityById(Long id) {
        return blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog bulunamadı: id=" + id));
    }

    // Client publishedAt'i acikca gonderirse oncelikli; gonderilmezse mevcut
    // "PUBLISHED'e gecince now(), degilse null" otomatik davranisi korunur
    // (existing==null create anlamina gelir, existing.getStatus() henuz eski
    // statudur cunku bu, blog.setStatus() cagrisindan ONCE cagrilir).
    private LocalDateTime resolvePublishedAt(BlogRequest request, Blog existing) {
        if (request.publishedAt() != null) {
            return request.publishedAt();
        }
        if (request.status() != BlogStatus.PUBLISHED) {
            return null;
        }
        if (existing == null || existing.getStatus() != BlogStatus.PUBLISHED) {
            return LocalDateTime.now();
        }
        return existing.getPublishedAt();
    }
}
