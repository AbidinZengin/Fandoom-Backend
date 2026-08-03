package com.example.fandoom_backend.blog.specification;

import com.example.fandoom_backend.blog.entity.Blog;
import com.example.fandoom_backend.blog.entity.BlogTag;
import com.example.fandoom_backend.tag.entity.Tag;
import com.example.fandoom_backend.tag.entity.TagAssignment;
import com.example.fandoom_backend.tag.entity.TagType;
import com.example.fandoom_backend.tag.entity.TaggableType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

// Blog hub facet filtreleri (Format/Franchise/Mood/Tema/Spoiler durumu) için
// Specification üretici. Format/Mood/Tema, TagAssignment (taggableType=BLOG,
// JPA ilişkisi YOK, taggableId manuel korelasyon) + Tag (type+slug) üzerinden
// korelasyonlu EXISTS alt-sorgusuyla yazılır; JOIN+distinct KULLANILMAZ,
// aksi halde bir blog'un birden çok eşleşen tag'i olduğunda satır çoğalır.
//
// İSTİSNA (bkz. CLAUDE.md "Bağımsızlık kuralları"): bu sınıf tag/ modülünün
// entity'lerine (Tag, TagAssignment) doğrudan erişir — normalde yasak olan
// bir cross-module entity erişimi. Specification/Criteria API, DTO/service
// interface üzerinden ifade edilemez (gerçek JPA metamodeline ihtiyaç duyar),
// bu yüzden bilinçli olarak kabul edilmiş bir istisnadır. Bunun dışında
// blog/ modülünün geri kalanı (BlogQueryServiceImpl dahil) tag/ modülüne
// yalnızca TagAssignmentService interface'i üzerinden erişir.
public final class BlogSpecificationBuilder {

    private BlogSpecificationBuilder() {
    }

    public static Specification<Blog> hasFormat(String formatSlug) {
        if (formatSlug == null || formatSlug.isBlank()) {
            return null;
        }
        return tagExists(TagType.FORMAT, List.of(formatSlug));
    }

    public static Specification<Blog> hasFranchise(Long franchiseId) {
        if (franchiseId == null) {
            return null;
        }
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<BlogTag> blogTagRoot = subquery.from(BlogTag.class);
            subquery.select(blogTagRoot.get("id"));
            subquery.where(
                    cb.equal(blogTagRoot.get("blog").get("id"), root.get("id")),
                    cb.equal(blogTagRoot.get("franchiseId"), franchiseId));
            return cb.exists(subquery);
        };
    }

    public static Specification<Blog> hasAnyMood(List<String> moodSlugs) {
        if (moodSlugs == null || moodSlugs.isEmpty()) {
            return null;
        }
        return tagExists(TagType.MOOD, moodSlugs);
    }

    public static Specification<Blog> hasAnyTheme(List<String> themeSlugs) {
        if (themeSlugs == null || themeSlugs.isEmpty()) {
            return null;
        }
        return tagExists(TagType.THEME, themeSlugs);
    }

    public static Specification<Blog> isSpoilerFree(Boolean spoilerFree) {
        if (spoilerFree == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("spoilerFree"), spoilerFree);
    }

    private static Specification<Blog> tagExists(TagType type, List<String> slugs) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<TagAssignment> assignmentRoot = subquery.from(TagAssignment.class);
            Join<TagAssignment, Tag> tagJoin = assignmentRoot.join("tag");
            subquery.select(assignmentRoot.get("id"));
            subquery.where(
                    cb.equal(assignmentRoot.get("taggableType"), TaggableType.BLOG),
                    cb.equal(assignmentRoot.get("taggableId"), root.get("id")),
                    cb.equal(tagJoin.get("type"), type),
                    tagJoin.get("slug").in(slugs));
            return cb.exists(subquery);
        };
    }
}
