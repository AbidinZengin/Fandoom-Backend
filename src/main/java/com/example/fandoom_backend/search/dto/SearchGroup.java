package com.example.fandoom_backend.search.dto;

import java.util.List;

public record SearchGroup(SearchType type, List<SearchHit> items) {}
