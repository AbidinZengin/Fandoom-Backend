package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.community.dto.FollowedTagResponse;
import com.example.fandoom_backend.community.dto.TagFollowStatusResponse;
import com.example.fandoom_backend.community.dto.TrendingTagResponse;

import java.util.List;

public interface TagFollowService {

    // İdempotent: zaten takip ediliyorsa hata vermez, mevcut durumu döner.
    TagFollowStatusResponse follow(Long userId, String tag);

    // İdempotent: takip edilmiyorsa hata vermez.
    TagFollowStatusResponse unfollow(Long userId, String tag);

    List<FollowedTagResponse> listFollowed(Long userId);

    // window: "7d" (varsayılan) | "30d" — job/cache YOK, on-the-fly agregasyon.
    List<TrendingTagResponse> getTrending(String window, int limit);
}
