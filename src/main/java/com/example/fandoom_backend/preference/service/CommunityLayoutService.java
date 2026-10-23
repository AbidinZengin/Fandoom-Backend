package com.example.fandoom_backend.preference.service;

import com.example.fandoom_backend.preference.dto.CommunityLayoutRequest;
import com.example.fandoom_backend.preference.dto.CommunityLayoutResponse;

import java.util.Optional;

public interface CommunityLayoutService {

    // Kayıt yoksa boş (controller 204 döner).
    Optional<CommunityLayoutResponse> get(Long userId);

    // Tam yazma (upsert, son yazan kazanır); updatedAt sunucu saatiyle.
    CommunityLayoutResponse put(Long userId, CommunityLayoutRequest request);
}
