package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.SaveItemRequest;
import com.example.fandoom_backend.account.dto.UpdateSavedItemRequest;
import com.example.fandoom_backend.account.dto.UserSavedItemResponse;

public interface UserSavedItemService {
    UserSavedItemResponse save(Long userId, SaveItemRequest request);
    UserSavedItemResponse update(Long userId, Long id, UpdateSavedItemRequest request);
    void delete(Long userId, Long id);
}
