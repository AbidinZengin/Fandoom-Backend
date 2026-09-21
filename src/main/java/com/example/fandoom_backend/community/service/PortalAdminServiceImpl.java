package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.DuplicateResourceException;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.dto.AdminPortalResponse;
import com.example.fandoom_backend.community.dto.PortalCreateRequest;
import com.example.fandoom_backend.community.dto.PortalUpdateRequest;
import com.example.fandoom_backend.community.entity.Portal;
import com.example.fandoom_backend.community.entity.PortalPostingPolicy;
import com.example.fandoom_backend.community.entity.PortalProduction;
import com.example.fandoom_backend.community.entity.PortalProductionType;
import com.example.fandoom_backend.community.entity.PortalStatus;
import com.example.fandoom_backend.community.mapper.PortalMapper;
import com.example.fandoom_backend.community.repository.PortalProductionRepository;
import com.example.fandoom_backend.community.repository.PortalRepository;
import com.example.fandoom_backend.media.service.MediaUrlValidator;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.service.SeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Tüm yazmalar thread cache'lerini (thread:list/thread:detail) temizler: yanıtlardaki portal.name ve
// HIDDEN-portal filtresi bu cache'lere gömülü. (Portal cache'i henüz yok.)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortalAdminServiceImpl implements PortalAdminService {

    private final PortalRepository portalRepository;
    private final PortalProductionRepository portalProductionRepository;
    private final PortalMapper portalMapper;
    private final MovieService movieService;
    private final SeriesService seriesService;
    private final MediaUrlValidator mediaUrlValidator;

    @Override
    public PageResponse<AdminPortalResponse> list(Pageable pageable) {
        // Client sıralaması yok sayılır (keyfi sort alanı enjeksiyonu yok): sortOrder, sonra id.
        Page<Portal> page = portalRepository.findAll(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))));
        Map<Long, List<String>> productions = productionSlugsByPortal(
                page.getContent().stream().map(Portal::getId).toList());
        return PageResponse.from(page.map(p ->
                portalMapper.toAdminResponse(p, productions.getOrDefault(p.getId(), List.of()))));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
    public AdminPortalResponse create(PortalCreateRequest request) {
        if (portalRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("Bu slug zaten kullanılıyor: " + request.slug());
        }
        Portal portal = Portal.builder()
                .slug(request.slug())
                .nameTr(request.nameTr().trim())
                .nameEn(request.nameEn().trim())
                .descriptionTr(blankToNull(request.descriptionTr()))
                .descriptionEn(blankToNull(request.descriptionEn()))
                .bannerUrl(validatedImageUrl(request.bannerUrl(), "bannerUrl"))
                .iconUrl(validatedImageUrl(request.iconUrl(), "iconUrl"))
                .accentColor(blankToNull(request.accentColor()))
                .postingPolicy(request.postingPolicy() == null ? PortalPostingPolicy.OPEN : request.postingPolicy())
                .status(request.status() == null ? PortalStatus.ACTIVE : request.status())
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .build();
        List<String> desired = distinct(request.productionSlugs());
        Map<String, PortalProductionType> types = resolveProductionTypes(desired);
        assertNotLinkedElsewhere(desired, null);

        portal = portalRepository.save(portal);
        addProductions(portal.getId(), desired, types);
        return toResponse(portal);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
    public AdminPortalResponse update(String slug, PortalUpdateRequest request) {
        Portal portal = findBySlug(slug);
        if (request.slug() != null && !request.slug().equals(portal.getSlug())) {
            throw new InvalidReferenceException("Portal slug'ı değiştirilemez");
        }
        if (request.nameTr() != null) {
            portal.setNameTr(request.nameTr().trim());
        }
        if (request.nameEn() != null) {
            portal.setNameEn(request.nameEn().trim());
        }
        if (request.descriptionTr() != null) {
            portal.setDescriptionTr(blankToNull(request.descriptionTr()));
        }
        if (request.descriptionEn() != null) {
            portal.setDescriptionEn(blankToNull(request.descriptionEn()));
        }
        if (request.bannerUrl() != null) {
            portal.setBannerUrl(validatedImageUrl(request.bannerUrl(), "bannerUrl"));
        }
        if (request.iconUrl() != null) {
            portal.setIconUrl(validatedImageUrl(request.iconUrl(), "iconUrl"));
        }
        if (request.accentColor() != null) {
            portal.setAccentColor(blankToNull(request.accentColor()));   // "" = rengi kaldır
        }
        if (request.postingPolicy() != null) {
            portal.setPostingPolicy(request.postingPolicy());
        }
        if (request.status() != null) {
            portal.setStatus(request.status());
        }
        if (request.sortOrder() != null) {
            portal.setSortOrder(request.sortOrder());
        }
        if (request.productionSlugs() != null) {
            replaceProductions(portal.getId(), distinct(request.productionSlugs()));
        }
        return toResponse(portal);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
    public AdminPortalResponse archive(String slug) {
        Portal portal = findBySlug(slug);
        rejectHidden(portal);
        portal.setStatus(PortalStatus.ARCHIVED);
        return toResponse(portal);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
    public AdminPortalResponse unarchive(String slug) {
        Portal portal = findBySlug(slug);
        rejectHidden(portal);
        portal.setStatus(PortalStatus.ACTIVE);
        return toResponse(portal);
    }

    // HIDDEN portalı archive/unarchive ile sessizce görünür yapmayalım: görünürlük kararı PATCH status ile açıkça verilir.
    private void rejectHidden(Portal portal) {
        if (portal.getStatus() == PortalStatus.HIDDEN) {
            throw new InvalidReferenceException("HIDDEN portal için status'ü PATCH ile değiştirin");
        }
    }

    // Diff tabanlı: yalnız çıkarılanlar silinir, yalnız eklenenler yazılır (unique(production_slug) çakışması yok).
    private void replaceProductions(Long portalId, List<String> desired) {
        Map<String, PortalProduction> existing = portalProductionRepository.findByPortalId(portalId).stream()
                .collect(Collectors.toMap(PortalProduction::getProductionSlug, pp -> pp));
        List<String> toRemove = existing.keySet().stream().filter(s -> !desired.contains(s)).toList();
        List<String> toAdd = desired.stream().filter(s -> !existing.containsKey(s)).toList();

        Map<String, PortalProductionType> types = resolveProductionTypes(toAdd);
        assertNotLinkedElsewhere(toAdd, portalId);
        if (!toRemove.isEmpty()) {
            portalProductionRepository.deleteByPortalIdAndProductionSlugIn(portalId, toRemove);
        }
        addProductions(portalId, toAdd, types);
    }

    private void addProductions(Long portalId, List<String> slugs, Map<String, PortalProductionType> types) {
        if (slugs.isEmpty()) {
            return;
        }
        portalProductionRepository.saveAll(slugs.stream()
                .map(s -> PortalProduction.builder().portalId(portalId).productionSlug(s)
                        .productionType(types.get(s)).build())
                .toList());
    }

    // Yapım varlığı doğrulanır (mevcut Movie/Series existsBySlug); slug ikisinde de varsa MOVIE önceliklidir.
    private Map<String, PortalProductionType> resolveProductionTypes(Collection<String> slugs) {
        Map<String, PortalProductionType> types = new HashMap<>();
        for (String s : slugs) {
            if (movieService.existsBySlug(s)) {
                types.put(s, PortalProductionType.MOVIE);
            } else if (seriesService.existsBySlug(s)) {
                types.put(s, PortalProductionType.SERIES);
            } else {
                throw new InvalidReferenceException("Geçersiz productionSlug: " + s);
            }
        }
        return types;
    }

    // Bir yapım en fazla bir portala bağlı (uk_portal_production_slug) — DB hatasından önce anlamlı 409.
    private void assertNotLinkedElsewhere(Collection<String> slugs, Long ownPortalId) {
        for (String s : slugs) {
            portalProductionRepository.findByProductionSlug(s)
                    .filter(pp -> !pp.getPortalId().equals(ownPortalId))
                    .ifPresent(pp -> {
                        throw new DuplicateResourceException("Yapım zaten başka bir portala bağlı: " + s);
                    });
        }
    }

    private String validatedImageUrl(String url, String field) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        if (!mediaUrlValidator.isOwnedImage(url)) {
            throw new InvalidReferenceException("Geçersiz " + field + " (yalnızca bu projenin storage'ına yüklenen görseller)");
        }
        return url;
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static List<String> distinct(List<String> values) {
        return values == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(values));
    }

    private Portal findBySlug(String slug) {
        return portalRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Portal bulunamadı: slug=" + slug));
    }

    private AdminPortalResponse toResponse(Portal portal) {
        return portalMapper.toAdminResponse(portal,
                productionSlugsByPortal(List.of(portal.getId())).getOrDefault(portal.getId(), List.of()));
    }

    // Sayfa başına tek IN sorgusu.
    private Map<Long, List<String>> productionSlugsByPortal(Collection<Long> portalIds) {
        if (portalIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<String>> result = new HashMap<>();
        portalProductionRepository.findByPortalIdIn(portalIds).stream()
                .sorted((a, b) -> a.getProductionSlug().compareTo(b.getProductionSlug()))
                .forEach(pp -> result.computeIfAbsent(pp.getPortalId(), k -> new ArrayList<>()).add(pp.getProductionSlug()));
        return result;
    }
}
