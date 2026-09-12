package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.CommentRequest;
import com.example.fandoom_backend.community.dto.CommentResponse;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    // viewerId: giriş yapmamış istekte null — isLiked hep false döner, ekstra
    // sorgu atılmaz.
    PageResponse<CommentResponse> listForThread(String threadSlug, String sort, Long viewerId, Pageable pageable);

    CommentResponse create(Long authorId, String threadSlug, CommentRequest request);

    void delete(Long userId, boolean moderator, Long commentId);
}
