package com.example.fandoom_backend.common.dto;

import java.time.LocalDate;
import java.util.List;

public record CursorPageResponse<T>(
        List<T> content, boolean hasNext, LocalDate nextCursorDate, Long nextCursorId) {}
