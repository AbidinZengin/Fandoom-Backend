package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.community.dto.PortalMembershipStatusResponse;

// Katıl / Ayrıl. İkisi de idempotent; memberCount yalnız gerçekten satır eklenince/silinince değişir.
public interface PortalMembershipService {

    // Yok/HIDDEN portal -> 404. ARCHIVED portala YENİ üyelik -> 400 (zaten üye olan için idempotent: member=true).
    PortalMembershipStatusResponse join(Long userId, String slug);

    // Yok/HIDDEN -> 404. ARCHIVED'dan ayrılmak serbest. Üye değilse no-op (member=false).
    PortalMembershipStatusResponse leave(Long userId, String slug);
}
