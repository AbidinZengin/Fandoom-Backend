package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.community.dto.ThreadBookmarkStatusResponse;
import com.example.fandoom_backend.community.dto.ThreadLikeStatusResponse;

public interface ThreadInteractionService {

    ThreadLikeStatusResponse like(Long userId, String slug);

    ThreadLikeStatusResponse unlike(Long userId, String slug);

    ThreadBookmarkStatusResponse bookmark(Long userId, String slug);

    ThreadBookmarkStatusResponse unbookmark(Long userId, String slug);
}
