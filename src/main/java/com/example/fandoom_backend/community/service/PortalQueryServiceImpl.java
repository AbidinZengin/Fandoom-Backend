package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.dto.PortalDetailResponse;
import com.example.fandoom_backend.community.dto.PortalSummaryResponse;
import com.example.fandoom_backend.community.entity.Portal;
import com.example.fandoom_backend.community.entity.PortalMembership;
import com.example.fandoom_backend.community.entity.PortalProduction;
import com.example.fandoom_backend.community.entity.PortalStatus;
import com.example.fandoom_backend.community.mapper.PortalMapper;
import com.example.fandoom_backend.community.repository.PortalMembershipRepository;
import com.example.fandoom_backend.community.repository.PortalProductionRepository;
import com.example.fandoom_backend.community.repository.PortalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// Cache: bilerek YOK. Yanıt isMember (kullanıcıya özel) ve dile göre çözülmüş ad içerir; tek global TTL'li paylaşılan
// cache'e girmemeli, anonim-only cache de portal yazmaları + thread/yorum/üyelik sayaçlarıyla sürekli bayatlardı.
// Portal tablosu küçük, sorgular sabit sayıda ve indeksli (bkz. PortalRepository) — ölçek sorunu çıkarsa anonim liste
// için CLAUDE.md "Redis Cache" kurallarıyla (sync=true, viewerId==null condition, portal yazmalarında evict) eklenir.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortalQueryServiceImpl implements PortalQueryService {

    // ---- Trending formülü: TEK tanım yeri ----
    // skor = THREAD_WEIGHT * (son 7 günde açılan PUBLISHED thread) + (son 7 günde PUBLISHED thread'lere yazılan PUBLISHED yorum)
    // Yeni alan/job YOK: her istekte on-the-fly, iki gruplu COUNT (idx_thread_status_created / idx_comment_status_created_subject).
    static final int THREAD_WEIGHT = 3;
    static final int TRENDING_WINDOW_DAYS = 7;

    static long trendingScore(long newThreads, long newComments) {
        return THREAD_WEIGHT * newThreads + newComments;
    }

    private static final Set<PortalStatus> PUBLIC_STATUSES = EnumSet.of(PortalStatus.ACTIVE, PortalStatus.ARCHIVED);
    private static final int MAX_QUERY_LENGTH = 100;
    private static final int MAX_PAGE_SIZE = 50;
    private static final char LIKE_ESCAPE = '!';

    private final PortalRepository portalRepository;
    private final PortalProductionRepository portalProductionRepository;
    private final PortalMembershipRepository portalMembershipRepository;
    private final PortalMapper portalMapper;

    @Override
    public PageResponse<PortalSummaryResponse> list(String sort, String q, Long viewerId, Pageable pageable) {
        String pattern = likePattern(q);
        int size = Math.clamp(pageable.getPageSize(), 1, MAX_PAGE_SIZE);
        int page = Math.max(pageable.getPageNumber(), 0);

        Page<Portal> portals = switch (normalizeSort(sort)) {
            case "members" -> portalRepository.search(PUBLIC_STATUSES, pattern, PageRequest.of(page, size,
                    Sort.by(Sort.Order.desc("memberCount"), Sort.Order.asc("id"))));
            case "new" -> portalRepository.search(PUBLIC_STATUSES, pattern, PageRequest.of(page, size,
                    Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
            case "alpha" -> portalRepository.search(PUBLIC_STATUSES, pattern, PageRequest.of(page, size,
                    Sort.by(Sort.Order.asc(isTurkish() ? "nameTr" : "nameEn"), Sort.Order.asc("id"))));
            default -> trending(pattern, PageRequest.of(page, size));
        };
        return PageResponse.from(new PageImpl<>(toSummaries(portals.getContent(), viewerId),
                portals.getPageable(), portals.getTotalElements()));
    }

    @Override
    public PortalDetailResponse getBySlug(String slug, Long viewerId) {
        Portal portal = portalRepository.findBySlug(slug)
                .filter(p -> p.getStatus() != PortalStatus.HIDDEN)
                .orElseThrow(() -> new ResourceNotFoundException("Portal bulunamadı: slug=" + slug));
        boolean member = viewerId != null && portalMembershipRepository.existsByPortalIdAndUserId(portal.getId(), viewerId);
        List<String> productions = productionSlugsByPortal(List.of(portal.getId())).getOrDefault(portal.getId(), List.of());
        return portalMapper.toDetail(portal, member, productions);
    }

    @Override
    public List<PortalSummaryResponse> listJoined(Long userId, String sort) {
        List<PortalMembership> memberships = portalMembershipRepository.findByUserIdOrderByJoinedAtDescPortalIdAsc(userId);
        if (memberships.isEmpty()) {
            return List.of();
        }
        Map<Long, Portal> byId = portalRepository.findAllById(memberships.stream().map(PortalMembership::getPortalId).toList())
                .stream().filter(p -> p.getStatus() != PortalStatus.HIDDEN)
                .collect(Collectors.toMap(Portal::getId, p -> p));
        List<Portal> ordered = memberships.stream().map(m -> byId.get(m.getPortalId())).filter(Objects::nonNull).toList();
        if (ordered.isEmpty()) {
            return List.of();
        }
        if ("activity".equals(sort)) {
            List<Long> ids = ordered.stream().map(Portal::getId).toList();
            LocalDateTime since = LocalDateTime.now().minusDays(TRENDING_WINDOW_DAYS);
            Map<Long, Long> threads = counts(portalRepository.countNewThreadsSince(ids, since));
            Map<Long, Long> comments = counts(portalRepository.countNewCommentsSince(ids, since));
            // sıralama kararlı (List.sort): eşit skorda joinedAt azalan sıra korunur
            List<Portal> sorted = new ArrayList<>(ordered);
            sorted.sort(Comparator.comparingLong((Portal p) -> trendingScore(
                    threads.getOrDefault(p.getId(), 0L), comments.getOrDefault(p.getId(), 0L))).reversed());
            ordered = sorted;
        }
        Map<Long, List<String>> productions = productionSlugsByPortal(ordered.stream().map(Portal::getId).toList());
        return ordered.stream()
                .map(p -> portalMapper.toSummary(p, true, productions.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    // Kümenin tamamı çekilir (portal sayısı küçük), skor iki gruplu COUNT ile hesaplanır, bellekte sıralanıp dilimlenir.
    // Eşitlikte: admin'in verdiği sortOrder, sonra id (deterministik).
    private Page<Portal> trending(String pattern, PageRequest pageRequest) {
        List<Portal> all = portalRepository.searchAll(PUBLIC_STATUSES, pattern);
        if (all.isEmpty()) {
            return new PageImpl<>(List.of(), pageRequest, 0);
        }
        List<Long> ids = all.stream().map(Portal::getId).toList();
        LocalDateTime since = LocalDateTime.now().minusDays(TRENDING_WINDOW_DAYS);
        Map<Long, Long> threads = counts(portalRepository.countNewThreadsSince(ids, since));
        Map<Long, Long> comments = counts(portalRepository.countNewCommentsSince(ids, since));

        Comparator<Portal> byScore = Comparator
                .comparingLong((Portal p) -> trendingScore(
                        threads.getOrDefault(p.getId(), 0L), comments.getOrDefault(p.getId(), 0L)))
                .reversed()
                .thenComparingInt(Portal::getSortOrder)
                .thenComparing(Portal::getId);
        List<Portal> sorted = all.stream().sorted(byScore).toList();

        int from = (int) Math.min(pageRequest.getOffset(), sorted.size());
        int to = Math.min(from + pageRequest.getPageSize(), sorted.size());
        return new PageImpl<>(sorted.subList(from, to), pageRequest, sorted.size());
    }

    private static Map<Long, Long> counts(List<Object[]> rows) {
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    // Sayfa başına: 1 üyelik sorgusu + 1 yapım sorgusu (N+1 yok).
    private List<PortalSummaryResponse> toSummaries(List<Portal> portals, Long viewerId) {
        if (portals.isEmpty()) {
            return List.of();
        }
        List<Long> ids = portals.stream().map(Portal::getId).toList();
        Set<Long> memberOf = viewerId == null
                ? Set.of()
                : portalMembershipRepository.findPortalIdsByUserIdAndPortalIdIn(viewerId, ids);
        Map<Long, List<String>> productions = productionSlugsByPortal(ids);
        return portals.stream()
                .map(p -> portalMapper.toSummary(p, memberOf.contains(p.getId()),
                        productions.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    private Map<Long, List<String>> productionSlugsByPortal(List<Long> portalIds) {
        return portalProductionRepository.findByPortalIdIn(portalIds).stream()
                .sorted(Comparator.comparing(PortalProduction::getProductionSlug))
                .collect(Collectors.groupingBy(PortalProduction::getPortalId,
                        Collectors.mapping(PortalProduction::getProductionSlug, Collectors.toList())));
    }

    private static String normalizeSort(String sort) {
        return sort == null ? "trending" : switch (sort) {
            case "members", "new", "alpha" -> sort;
            default -> "trending";
        };
    }

    private static boolean isTurkish() {
        return "tr".equalsIgnoreCase(LocaleContextHolder.getLocale().getLanguage());
    }

    // q boş -> "%" (hepsi). Aksi halde '!' , '%' , '_' escape edilir (kullanıcı wildcard enjekte edemez).
    static String likePattern(String q) {
        if (!StringUtils.hasText(q)) {
            return "%";
        }
        String trimmed = q.trim();
        if (trimmed.length() > MAX_QUERY_LENGTH) {
            throw new InvalidReferenceException("q en fazla " + MAX_QUERY_LENGTH + " karakter olabilir");
        }
        StringBuilder sb = new StringBuilder("%");
        for (char c : trimmed.toLowerCase(Locale.ROOT).toCharArray()) {
            if (c == LIKE_ESCAPE || c == '%' || c == '_') {
                sb.append(LIKE_ESCAPE);
            }
            sb.append(c);
        }
        return sb.append('%').toString();
    }
}
