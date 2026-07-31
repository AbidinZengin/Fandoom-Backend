package com.example.fandoom_backend.contentdrop.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.common.util.SlugGenerator;
import com.example.fandoom_backend.contentdrop.dto.ContentDropBlockRequest;
import com.example.fandoom_backend.contentdrop.dto.ContentDropDetailResponse;
import com.example.fandoom_backend.contentdrop.dto.ContentDropRequest;
import com.example.fandoom_backend.contentdrop.dto.ContentDropSummaryResponse;
import com.example.fandoom_backend.contentdrop.dto.ContentDropTagRequest;
import com.example.fandoom_backend.contentdrop.entity.ContentDrop;
import com.example.fandoom_backend.contentdrop.entity.ContentDropBlock;
import com.example.fandoom_backend.contentdrop.entity.ContentDropRelation;
import com.example.fandoom_backend.contentdrop.entity.ContentDropStatus;
import com.example.fandoom_backend.contentdrop.entity.ContentDropTag;
import com.example.fandoom_backend.contentdrop.entity.SubjectType;
import com.example.fandoom_backend.contentdrop.mapper.ContentDropMapper;
import com.example.fandoom_backend.contentdrop.repository.ContentDropRelationRepository;
import com.example.fandoom_backend.contentdrop.repository.ContentDropRepository;
import com.example.fandoom_backend.contentdrop.repository.ContentDropTagRepository;
import com.example.fandoom_backend.franchise.service.FranchiseService;
import com.example.fandoom_backend.media.service.ImageStorageService;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.service.SeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
public class ContentDropServiceImpl implements ContentDropService {

    // Bir content drop sayfasının altında gösterilecek sabit vitrin boyutu
    // (kürasyon + tag merdiveni + recency/popülerlik ile doldurulur).
    private static final int RELATED_SHOWCASE_SIZE = 6;
    private static final int WORDS_PER_MINUTE = 200;

    private final ContentDropRepository contentDropRepository;
    private final ContentDropTagRepository contentDropTagRepository;
    private final ContentDropRelationRepository contentDropRelationRepository;
    private final ContentDropMapper contentDropMapper;
    private final MovieService movieService;
    private final SeriesService seriesService;
    private final FranchiseService franchiseService;
    private final ImageStorageService imageStorageService;

    @Override
    public PageResponse<ContentDropSummaryResponse> list(Pageable pageable) {
        Page<ContentDropSummaryResponse> page = contentDropRepository.findAll(pageable)
                .map(contentDropMapper::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Override
    public ContentDropDetailResponse getById(Long id) {
        ContentDrop contentDrop = findEntityById(id);
        return contentDropMapper.toDetailResponse(contentDrop, resolveRelated(contentDrop));
    }

    @Override
    @Transactional
    public ContentDropDetailResponse getBySlug(String slug) {
        ContentDrop contentDrop = contentDropRepository.findBySlugAndStatus(slug, ContentDropStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Content drop bulunamadı: slug=" + slug));
        contentDropRepository.incrementViewCount(contentDrop.getId());
        contentDrop.setViewCount(contentDrop.getViewCount() + 1);
        return contentDropMapper.toDetailResponse(contentDrop, resolveRelated(contentDrop));
    }

    @Override
    public List<ContentDropSummaryResponse> findRelatedForProduction(
            SubjectType productionType, String productionSlug,
            Integer seasonNumber, Integer episodeNumber, int limit) {
        Long subjectId = resolveProductionId(productionType, productionSlug);
        Long franchiseId = resolveFranchiseId(productionType, subjectId);

        List<ContentDrop> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        collectSpecificMatches(result, seen, productionType, subjectId, franchiseId, seasonNumber, episodeNumber, limit);
        fillWithPopular(result, seen, limit);
        return contentDropMapper.toSummaryResponseList(result);
    }

    @Override
    @Transactional
    public ContentDropDetailResponse create(ContentDropRequest request) {
        ContentDrop contentDrop = ContentDrop.builder()
                .title(request.title())
                .slug(SlugGenerator.generateUnique(request.title(), contentDropRepository::existsBySlug))
                .kicker(request.kicker())
                .axis(request.axis())
                .imageUrl(request.imageUrl())
                .imageUrlLarge(request.imageUrlLarge())
                .imageAlt(request.imageAlt())
                .spoilerThroughSeasonNumber(request.spoilerThroughSeasonNumber())
                .spoilerThroughEpisodeNumber(request.spoilerThroughEpisodeNumber())
                .status(request.status())
                .publishedAt(request.status() == ContentDropStatus.PUBLISHED ? LocalDateTime.now() : null)
                .build();
        applyBlocks(contentDrop, request.blocks());
        applyTags(contentDrop, request.tags());
        contentDrop = contentDropRepository.save(contentDrop);
        return contentDropMapper.toDetailResponse(contentDrop, resolveRelated(contentDrop));
    }

    @Override
    @Transactional
    public ContentDropDetailResponse update(Long id, ContentDropRequest request) {
        ContentDrop contentDrop = findEntityById(id);
        imageStorageService.deleteIfChanged(contentDrop.getImageUrl(), request.imageUrl());
        imageStorageService.deleteIfChanged(contentDrop.getImageUrlLarge(), request.imageUrlLarge());
        if (!contentDrop.getTitle().equals(request.title())) {
            contentDrop.setSlug(SlugGenerator.generateUnique(request.title(),
                    slug -> contentDropRepository.existsBySlugAndIdNot(slug, id)));
        }
        contentDrop.setTitle(request.title());
        contentDrop.setKicker(request.kicker());
        contentDrop.setAxis(request.axis());
        contentDrop.setImageUrl(request.imageUrl());
        contentDrop.setImageUrlLarge(request.imageUrlLarge());
        contentDrop.setImageAlt(request.imageAlt());
        contentDrop.setSpoilerThroughSeasonNumber(request.spoilerThroughSeasonNumber());
        contentDrop.setSpoilerThroughEpisodeNumber(request.spoilerThroughEpisodeNumber());
        if (contentDrop.getStatus() != ContentDropStatus.PUBLISHED && request.status() == ContentDropStatus.PUBLISHED) {
            contentDrop.setPublishedAt(LocalDateTime.now());
        } else if (request.status() != ContentDropStatus.PUBLISHED) {
            contentDrop.setPublishedAt(null);
        }
        contentDrop.setStatus(request.status());
        applyBlocks(contentDrop, request.blocks());
        applyTags(contentDrop, request.tags());
        return contentDropMapper.toDetailResponse(contentDrop, resolveRelated(contentDrop));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ContentDrop contentDrop = findEntityById(id);
        imageStorageService.delete(contentDrop.getImageUrl());
        imageStorageService.delete(contentDrop.getImageUrlLarge());
        contentDropRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return contentDropRepository.existsById(id);
    }

    @Override
    @Transactional
    public List<ContentDropSummaryResponse> replaceRelated(Long id, List<Long> relatedContentDropIds) {
        ContentDrop source = findEntityById(id);
        if (relatedContentDropIds.contains(id)) {
            throw new InvalidReferenceException("Bir content drop kendisiyle ilişkilendirilemez: id=" + id);
        }
        List<ContentDrop> targets = relatedContentDropIds.stream().distinct().map(this::findEntityById).toList();

        source.clearOutgoingRelations();
        int orderIndex = 0;
        for (ContentDrop target : targets) {
            source.addOutgoingRelation(ContentDropRelation.builder()
                    .relatedContentDrop(target)
                    .orderIndex(orderIndex++)
                    .build());
        }
        return contentDropMapper.toSummaryResponseList(targets);
    }

    // ---- kademeli-geri-düşüş merdiveni ----

    private void collectSpecificMatches(List<ContentDrop> result, Set<Long> seen,
                                         SubjectType subjectType, Long subjectId, Long franchiseId,
                                         Integer seasonNumber, Integer episodeNumber, int limit) {
        if (subjectType != null && subjectId != null) {
            if (episodeNumber != null) {
                collect(result, seen, contentDropTagRepository
                        .findByExactEpisode(subjectType, subjectId, seasonNumber, episodeNumber), limit);
            }
            if (result.size() < limit && seasonNumber != null) {
                collect(result, seen, contentDropTagRepository
                        .findBySameSeason(subjectType, subjectId, seasonNumber), limit);
            }
            if (result.size() < limit) {
                collect(result, seen, contentDropTagRepository
                        .findBySameProduction(subjectType, subjectId), limit);
            }
        }
        if (result.size() < limit && franchiseId != null) {
            collect(result, seen, contentDropTagRepository.findBySameFranchise(franchiseId), limit);
        }
    }

    private void fillWithPopular(List<ContentDrop> result, Set<Long> seen, int limit) {
        if (result.size() >= limit) {
            return;
        }
        int remaining = limit - result.size();
        List<ContentDrop> popular = seen.isEmpty()
                ? contentDropRepository.findByStatusOrderByPublishedAtDescViewCountDesc(
                        ContentDropStatus.PUBLISHED, PageRequest.of(0, remaining))
                : contentDropRepository.findByStatusAndIdNotInOrderByPublishedAtDescViewCountDesc(
                        ContentDropStatus.PUBLISHED, seen, PageRequest.of(0, remaining));
        collect(result, seen, popular, limit);
    }

    private void collect(List<ContentDrop> result, Set<Long> seen, List<ContentDrop> candidates, int limit) {
        for (ContentDrop candidate : candidates) {
            if (result.size() >= limit) {
                return;
            }
            if (seen.add(candidate.getId())) {
                result.add(candidate);
            }
        }
    }

    // Content drop sayfasının kendi altındaki "ilgili içerikler": önce editörün
    // ContentDropRelation ile elle seçtiği liste, boşsa/eksikse bu içeriğin KENDİ
    // tag'lerinden yürütülen merdivenle, o da yetmezse recency+popülerlik ile
    // tamamlanır.
    private List<ContentDropSummaryResponse> resolveRelated(ContentDrop contentDrop) {
        Set<Long> seen = new HashSet<>();
        seen.add(contentDrop.getId());
        List<ContentDrop> result = new ArrayList<>();

        for (ContentDropRelation relation : contentDropRelationRepository
                .findBySourceContentDropIdOrderByOrderIndexAsc(contentDrop.getId())) {
            if (result.size() >= RELATED_SHOWCASE_SIZE) {
                break;
            }
            ContentDrop related = relation.getRelatedContentDrop();
            if (related.getStatus() == ContentDropStatus.PUBLISHED && seen.add(related.getId())) {
                result.add(related);
            }
        }

        if (result.size() < RELATED_SHOWCASE_SIZE) {
            for (ContentDropTag tag : contentDrop.getTags()) {
                if (result.size() >= RELATED_SHOWCASE_SIZE) {
                    break;
                }
                collectSpecificMatches(result, seen, tag.getSubjectType(), tag.getSubjectId(), tag.getFranchiseId(),
                        tag.getSeasonNumber(), tag.getEpisodeNumber(), RELATED_SHOWCASE_SIZE);
            }
        }

        fillWithPopular(result, seen, RELATED_SHOWCASE_SIZE);
        return contentDropMapper.toSummaryResponseList(result);
    }

    // ---- yardımcılar ----

    private void applyBlocks(ContentDrop contentDrop, List<ContentDropBlockRequest> requests) {
        contentDrop.clearBlocks();
        if (requests == null) {
            contentDrop.setReadingTimeMinutes(null);
            return;
        }
        int orderIndex = 0;
        int wordCount = 0;
        for (ContentDropBlockRequest request : requests) {
            contentDrop.addBlock(ContentDropBlock.builder()
                    .orderIndex(orderIndex++)
                    .blockType(request.blockType())
                    .text(request.text())
                    .imageUrl(request.imageUrl())
                    .imageAlt(request.imageAlt())
                    .build());
            if (request.text() != null && !request.text().isBlank()) {
                wordCount += request.text().trim().split("\\s+").length;
            }
        }
        contentDrop.setReadingTimeMinutes(
                wordCount == 0 ? null : Math.max(1, Math.round(wordCount / (float) WORDS_PER_MINUTE)));
    }

    private void applyTags(ContentDrop contentDrop, List<ContentDropTagRequest> requests) {
        contentDrop.clearTags();
        if (requests == null) {
            return;
        }
        for (ContentDropTagRequest request : requests) {
            boolean hasSubject = request.subjectType() != null && request.subjectId() != null;
            boolean hasFranchise = request.franchiseId() != null;
            if (hasSubject == hasFranchise) {
                throw new InvalidReferenceException(
                        "Tag ya subjectType+subjectId ya da yalnızca franchiseId taşımalı: " + request);
            }
            if (hasSubject) {
                boolean exists = switch (request.subjectType()) {
                    case MOVIE -> movieService.existsById(request.subjectId());
                    case SERIES -> seriesService.existsById(request.subjectId());
                };
                if (!exists) {
                    throw new InvalidReferenceException(
                            "Geçersiz " + request.subjectType() + " id: " + request.subjectId());
                }
            } else if (!franchiseService.existsById(request.franchiseId())) {
                throw new InvalidReferenceException("Geçersiz franchise id: " + request.franchiseId());
            }
            contentDrop.addTag(ContentDropTag.builder()
                    .subjectType(request.subjectType())
                    .subjectId(request.subjectId())
                    .seasonNumber(request.seasonNumber())
                    .episodeNumber(request.episodeNumber())
                    .franchiseId(request.franchiseId())
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

    private ContentDrop findEntityById(Long id) {
        return contentDropRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content drop bulunamadı: id=" + id));
    }
}
