package com.example.fandoom_backend.community.dto;

// join/leave yanıtı: member = işlem sonrası üyelik durumu, memberCount = işlem sonrası üye sayısı.
public record PortalMembershipStatusResponse(boolean member, int memberCount) {
}
