package com.example.fandoom_backend.community.specification;

import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;

// BlogSpecificationBuilder ile aynı desen: her facet metodu "seçilmemiş"
// durumunu null dönerek ifade eder — ThreadServiceImpl bunları
// Specification.allOf(...) ile birleştirmeden önce null'ları eler.
public final class ThreadSpecificationBuilder {

    private ThreadSpecificationBuilder() {
    }

    public static Specification<Thread> isPublished() {
        return (root, query, cb) -> cb.equal(root.get("status"), ThreadStatus.PUBLISHED);
    }

    public static Specification<Thread> hasSurface(ThreadSurface surface) {
        if (surface == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("surface"), surface);
    }

    public static Specification<Thread> hasPortalId(Long portalId) {
        if (portalId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("portalId"), portalId);
    }

    public static Specification<Thread> hasPortalIdIn(Collection<Long> portalIds) {
        if (portalIds == null) {
            return null;
        }
        return (root, query, cb) -> root.get("portalId").in(portalIds);
    }

    // HIDDEN portalların thread'lerini dışlar; hiç HIDDEN portal yoksa (olağan durum) predicate hiç eklenmez.
    public static Specification<Thread> portalIdNotIn(Collection<Long> hiddenPortalIds) {
        if (hiddenPortalIds == null || hiddenPortalIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> cb.not(root.get("portalId").in(hiddenPortalIds));
    }

    public static Specification<Thread> hasProductionSlug(String productionSlug) {
        if (productionSlug == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("productionSlug"), productionSlug);
    }

    public static Specification<Thread> hasIdIn(List<Long> ids) {
        if (ids == null) {
            return null;
        }
        return (root, query, cb) -> root.get("id").in(ids);
    }
}
