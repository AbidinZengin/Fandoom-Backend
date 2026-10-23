package com.example.fandoom_backend.preference.dto;

import java.time.Instant;

public record CommunityLayoutResponse(int leftWidth, int rightWidth, String leftMode, String rightMode,
                                      Instant updatedAt) {
}
