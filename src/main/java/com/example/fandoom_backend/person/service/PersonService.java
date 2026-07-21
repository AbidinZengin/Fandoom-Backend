package com.example.fandoom_backend.person.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.person.dto.PersonDetailResponse;
import com.example.fandoom_backend.person.dto.PersonRequest;
import com.example.fandoom_backend.person.dto.PersonSummaryResponse;
import org.springframework.data.domain.Pageable;

public interface PersonService {
    PageResponse<PersonSummaryResponse> list(Pageable pageable);
    PersonDetailResponse getById(Long id);
    PersonDetailResponse getBySlug(String slug);
    PersonDetailResponse create(PersonRequest request);
    PersonDetailResponse update(Long id, PersonRequest request);
    void delete(Long id);
}
