package com.example.fandoom_backend.blog.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReplaceRelatedRequest(@NotNull List<@NotNull Long> relatedBlogIds) {
}
