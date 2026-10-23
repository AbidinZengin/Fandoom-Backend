package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.account.service.ProfilePrivacyLookup;
import com.example.fandoom_backend.community.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfilePrivacyLookupImpl implements ProfilePrivacyLookup {

    private final UserProfileRepository userProfileRepository;

    @Override
    public boolean isPrivate(Long userId) {
        return userProfileRepository.existsByUserIdAndPrivateProfileTrue(userId);
    }
}
