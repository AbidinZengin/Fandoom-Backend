package com.example.fandoom_backend.cms.service;

import com.example.fandoom_backend.cms.dto.HomeBlockRequest;
import com.example.fandoom_backend.cms.dto.HomeBlockResponse;
import com.example.fandoom_backend.cms.entity.PageName;

import java.util.List;

public interface HomeBlockService {
    List<HomeBlockResponse> getByPage(PageName page, Long entityId);
    HomeBlockResponse create(HomeBlockRequest request);
    HomeBlockResponse update(Long id, HomeBlockRequest request);
    void delete(Long id);
}
