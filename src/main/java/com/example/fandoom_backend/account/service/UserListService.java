package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.CreateUserListRequest;
import com.example.fandoom_backend.account.dto.UserListDetailResponse;
import com.example.fandoom_backend.account.dto.UserListSummaryResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserListService {
    List<UserListSummaryResponse> list(Long userId);
    UserListDetailResponse getById(Long userId, Long id, Pageable pageable);
    UserListDetailResponse create(Long userId, CreateUserListRequest request);
    UserListDetailResponse update(Long userId, Long id, CreateUserListRequest request);
    void delete(Long userId, Long id);
    UserListSummaryResponse togglePin(Long userId, Long id);
    List<UserListSummaryResponse> listPublicPinnedByUserId(Long userId);
}
