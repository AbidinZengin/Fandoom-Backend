package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.dto.PortalMembershipStatusResponse;
import com.example.fandoom_backend.community.entity.Portal;
import com.example.fandoom_backend.community.entity.PortalStatus;
import com.example.fandoom_backend.community.repository.PortalMembershipRepository;
import com.example.fandoom_backend.community.repository.PortalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Eşzamanlılık: portal satırı SELECT ... FOR UPDATE ile kilitlenir (bkz. PortalRepository.findWithLockBySlug), sonra
// membership INSERT IGNORE / DELETE'in etkilenen satır sayısına bakılarak sayaç native UPDATE ile değiştirilir.
// Kilit sırası hep portal -> membership: DB'deki FK (portal_membership -> portal) INSERT'te portal satırına S-kilit
// alır; kilitsiz olsaydı iki join'in S->X yükseltmesi deadlock'a düşerdi. Yanıttaki memberCount kilit altında okunan
// güncel değerin ±1'idir (entity alanı bilerek set edilmez — Thread sayaç deseni).
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortalMembershipServiceImpl implements PortalMembershipService {

    private final PortalRepository portalRepository;
    private final PortalMembershipRepository portalMembershipRepository;

    @Override
    @Transactional
    public PortalMembershipStatusResponse join(Long userId, String slug) {
        Portal portal = lockVisible(slug);
        if (portal.getStatus() == PortalStatus.ARCHIVED
                && !portalMembershipRepository.existsByPortalIdAndUserId(portal.getId(), userId)) {
            throw new InvalidReferenceException("Portal arşivlenmiş, yeni üye alınmıyor: " + slug);
        }
        int inserted = portalMembershipRepository.insertIgnore(portal.getId(), userId, LocalDateTime.now());
        if (inserted > 0) {
            portalRepository.incrementMemberCount(portal.getId());
        }
        return new PortalMembershipStatusResponse(true, portal.getMemberCount() + (inserted > 0 ? 1 : 0));
    }

    @Override
    @Transactional
    public PortalMembershipStatusResponse leave(Long userId, String slug) {
        Portal portal = lockVisible(slug);
        int deleted = portalMembershipRepository.deleteMembership(portal.getId(), userId);
        if (deleted > 0) {
            portalRepository.decrementMemberCount(portal.getId());
        }
        return new PortalMembershipStatusResponse(false, Math.max(0, portal.getMemberCount() - (deleted > 0 ? 1 : 0)));
    }

    private Portal lockVisible(String slug) {
        return portalRepository.findWithLockBySlug(slug)
                .filter(p -> p.getStatus() != PortalStatus.HIDDEN)
                .orElseThrow(() -> new ResourceNotFoundException("Portal bulunamadı: slug=" + slug));
    }
}
