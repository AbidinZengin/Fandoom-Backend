package com.example.fandoom_backend.account.dto;

import com.example.fandoom_backend.account.entity.FollowState;

// following == (status == FOLLOWING). REQUESTED, gizli profil onay akışında kullanılır.
public record FollowStateResponse(boolean following, FollowState status, long followerCount) {
}
