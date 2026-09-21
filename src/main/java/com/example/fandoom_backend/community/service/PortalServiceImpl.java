package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.dto.PortalRefResponse;
import com.example.fandoom_backend.community.entity.Portal;
import com.example.fandoom_backend.community.entity.PortalPostingPolicy;
import com.example.fandoom_backend.community.entity.PortalStatus;
import com.example.fandoom_backend.community.mapper.PortalMapper;
import com.example.fandoom_backend.community.repository.PortalMembershipRepository;
import com.example.fandoom_backend.community.repository.PortalProductionRepository;
import com.example.fandoom_backend.community.repository.PortalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortalServiceImpl implements PortalService {

    private final PortalRepository portalRepository;
    private final PortalProductionRepository portalProductionRepository;
    private final PortalMembershipRepository portalMembershipRepository;
    private final PortalMapper portalMapper;

    @Override
    public Long resolveVisibleId(String slug) {
        return findVisible(slug).getId();
    }

    @Override
    public Long resolvePostingPortalId(String slug, boolean staff) {
        Portal portal = findVisible(slug);
        if (portal.getStatus() == PortalStatus.ARCHIVED) {
            throw new InvalidReferenceException("Portal arşivlenmiş, yeni thread yazılamaz: " + slug);
        }
        if (portal.getPostingPolicy() == PortalPostingPolicy.STAFF_ONLY && !staff) {
            throw new AccessDeniedException("Bu portala yalnızca moderatör/admin thread yazabilir");
        }
        return portal.getId();
    }

    @Override
    public Long resolveMoveTargetId(String slug) {
        Portal portal = findVisible(slug);
        if (portal.getStatus() == PortalStatus.ARCHIVED) {
            throw new InvalidReferenceException("Portal arşivlenmiş, thread taşınamaz: " + slug);
        }
        return portal.getId();
    }

    @Override
    public Set<Long> getJoinedVisiblePortalIds(Long userId) {
        return portalMembershipRepository.findVisiblePortalIdsByUserId(userId);
    }

    @Override
    public void assertProductionBelongsToPortal(Long portalId, String productionSlug) {
        if (productionSlug == null) {
            return;
        }
        if (!portalProductionRepository.existsByPortalIdAndProductionSlug(portalId, productionSlug)) {
            throw new InvalidReferenceException("productionSlug seçilen portalın yapımlarından biri değil: " + productionSlug);
        }
    }

    @Override
    public Map<Long, PortalRefResponse> getRefsByIds(Collection<Long> portalIds) {
        Set<Long> ids = new HashSet<>(portalIds);
        Map<Long, PortalRefResponse> result = new HashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        portalRepository.findAllById(ids).stream().filter(p -> p.getStatus() != PortalStatus.HIDDEN)
                .forEach(p -> result.put(p.getId(), portalMapper.toRef(p)));
        return result;
    }

    @Override
    public Set<Long> getHiddenPortalIds() {
        return new HashSet<>(portalRepository.findIdsByStatus(PortalStatus.HIDDEN));
    }

    @Override
    @Transactional
    public void moveThreadCount(Long fromPortalId, Long toPortalId) {
        if (fromPortalId.equals(toPortalId)) {
            return;
        }
        if (fromPortalId < toPortalId) {
            portalRepository.decrementThreadCount(fromPortalId);
            portalRepository.incrementThreadCount(toPortalId);
        } else {
            portalRepository.incrementThreadCount(toPortalId);
            portalRepository.decrementThreadCount(fromPortalId);
        }
    }

    @Override
    @Transactional
    public void incrementThreadCount(Long portalId) {
        portalRepository.incrementThreadCount(portalId);
    }

    @Override
    @Transactional
    public void decrementThreadCount(Long portalId) {
        portalRepository.decrementThreadCount(portalId);
    }

    private Portal findVisible(String slug) {
        return portalRepository.findBySlug(slug)
                .filter(p -> p.getStatus() != PortalStatus.HIDDEN)
                .orElseThrow(() -> new ResourceNotFoundException("Portal bulunamadı: slug=" + slug));
    }
}
