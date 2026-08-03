package com.example.fandoom_backend.blog.service;

import com.example.fandoom_backend.blog.dto.BlogFilterCriteria;
import com.example.fandoom_backend.blog.dto.BlogFilterableSummaryResponse;
import com.example.fandoom_backend.blog.entity.Blog;
import com.example.fandoom_backend.blog.entity.BlogStatus;
import com.example.fandoom_backend.blog.entity.BlogTag;
import com.example.fandoom_backend.blog.repository.BlogRepository;
import com.example.fandoom_backend.blog.sort.BlogSortStrategyRegistry;
import com.example.fandoom_backend.blog.specification.BlogSpecificationBuilder;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.franchise.service.FranchiseService;
import com.example.fandoom_backend.tag.dto.TagAssignmentResponse;
import com.example.fandoom_backend.tag.entity.TagType;
import com.example.fandoom_backend.tag.entity.TaggableType;
import com.example.fandoom_backend.tag.service.TagAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlogQueryServiceImpl implements BlogQueryService {

    private final BlogRepository blogRepository;
    private final TagAssignmentService tagAssignmentService;
    private final FranchiseService franchiseService;
    private final BlogSortStrategyRegistry blogSortStrategyRegistry;

    @Override
    public PageResponse<BlogFilterableSummaryResponse> findFilterable(
            BlogFilterCriteria criteria, String sortKey, Pageable pageable) {
        Long franchiseId = resolveFranchiseId(criteria.franchiseSlug());

        // NOT: Specification.allOf(Specification...) varargs'ı, Spring Data JPA'da
        // null bir Specification ELEMANINI (spec'in kendisi, toPredicate()'in
        // DÖNÜŞ değeri değil) tolere ETMEZ — and(null) IllegalArgumentException
        // fırlatır. BlogSpecificationBuilder'daki her facet metodu ise "bu facet
        // seçilmemiş" durumunu tam olarak null DÖNEREK ifade ediyor (bkz. o
        // sınıfın Javadoc'u). Bu yüzden allOf'a vermeden önce null'ları burada
        // filtrelemek zorunludur; aksi halde hub'ın en yaygın senaryosu (hiçbir
        // facet seçilmemiş / yalnızca bazıları seçilmiş) her zaman 500 döner.
        Specification<Blog> specification = Specification.allOf(
                Stream.of(
                                BlogSpecificationBuilder.hasFormat(criteria.format()),
                                BlogSpecificationBuilder.hasFranchise(franchiseId),
                                BlogSpecificationBuilder.hasAnyMood(criteria.moodSlugs()),
                                BlogSpecificationBuilder.hasAnyTheme(criteria.themeSlugs()),
                                BlogSpecificationBuilder.isSpoilerFree(criteria.spoilerFree()),
                                onlyPublished())
                        .filter(Objects::nonNull)
                        .toList());

        Sort sort = blogSortStrategyRegistry.resolve(sortKey);
        Pageable pageableWithSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<BlogFilterableSummaryResponse> page = blogRepository.findAll(specification, pageableWithSort)
                .map(this::toFilterableSummary);
        return PageResponse.from(page);
    }

    // Hub, mevcut public sorgularla (findBySameFranchise vb.) tutarlı olarak
    // yalnızca yayınlanmış içerikleri gösterir. Bu, client tarafından
    // seçilebilir bir facet olmadığı için BlogSpecificationBuilder'ın
    // enumerasyonuna dahil edilmedi, sabit bir taban kısıt olarak burada eklendi.
    private Specification<Blog> onlyPublished() {
        return (root, query, cb) -> cb.equal(root.get("status"), BlogStatus.PUBLISHED);
    }

    private Long resolveFranchiseId(String franchiseSlug) {
        if (franchiseSlug == null || franchiseSlug.isBlank()) {
            return null;
        }
        return franchiseService.getBySlug(franchiseSlug).id();
    }

    // NOT: her blog için ayrı bir TagAssignment sorgusu çalıştırılıyor (N+1).
    // Hub sayfa boyutu küçük/orta (varsayılan 20) olduğu için v1 kapsamında
    // kabul edilebilir; ölçek sorunu çıkarsa taggableId IN (...) ile toplu
    // bir sorguya (ve Tag için JOIN FETCH'e) geçilmeli.
    private BlogFilterableSummaryResponse toFilterableSummary(Blog blog) {
        List<TagAssignmentResponse> assignments =
                tagAssignmentService.listForTarget(TaggableType.BLOG, blog.getId());

        String format = assignments.stream()
                .filter(a -> a.tagType() == TagType.FORMAT)
                .map(TagAssignmentResponse::tagSlug)
                .findFirst()
                .orElse(null);
        List<String> moods = assignments.stream()
                .filter(a -> a.tagType() == TagType.MOOD)
                .map(TagAssignmentResponse::tagSlug)
                .toList();
        List<String> themes = assignments.stream()
                .filter(a -> a.tagType() == TagType.THEME)
                .map(TagAssignmentResponse::tagSlug)
                .toList();

        String franchiseSlug = blog.getTags().stream()
                .map(BlogTag::getFranchiseId)
                .filter(Objects::nonNull)
                .findFirst()
                .map(id -> franchiseService.getById(id).slug())
                .orElse(null);

        return new BlogFilterableSummaryResponse(
                blog.getId(), blog.getSlug(), blog.getTitle(),
                blog.getImageUrl(), blog.getImageAlt(), blog.getReadingTimeMinutes(),
                blog.getPublishedAt(), blog.getViewCount(),
                format, franchiseSlug, moods, themes, blog.isSpoilerFree());
    }
}
