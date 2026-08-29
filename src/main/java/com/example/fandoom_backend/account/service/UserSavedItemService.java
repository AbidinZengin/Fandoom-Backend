package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.SaveItemRequest;
import com.example.fandoom_backend.account.dto.SavedItemStatusResponse;
import com.example.fandoom_backend.account.dto.UpdateSavedItemRequest;
import com.example.fandoom_backend.account.dto.UserSavedItemResponse;
import com.example.fandoom_backend.account.entity.ListType;
import com.example.fandoom_backend.account.entity.SavedItemType;

public interface UserSavedItemService {
    UserSavedItemResponse save(Long userId, SaveItemRequest request);
    UserSavedItemResponse update(Long userId, Long id, UpdateSavedItemRequest request);
    void delete(Long userId, Long id);
    // listType null ise itemType'ın VARSAYILAN sistem listesine (BLOG->READLIST,
    // MOVIE|SERIES->WATCHLIST) göre "kayıtlı mı" durumu döner; verilirse
    // (ör. WATCHED) o listeye göre kontrol eder — bkz. SavedItemStatusResponse.
    SavedItemStatusResponse getStatus(Long userId, SavedItemType itemType, Long itemId, ListType listType);
}
