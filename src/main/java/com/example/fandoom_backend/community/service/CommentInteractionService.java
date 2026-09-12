package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.community.dto.CommentLikeStatusResponse;

public interface CommentInteractionService {

    CommentLikeStatusResponse like(Long userId, Long commentId);

    CommentLikeStatusResponse unlike(Long userId, Long commentId);
}
