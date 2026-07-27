package com.example.fandoom_backend.user.dto;

import java.time.LocalDateTime;

// premiumExpiresAt kasıtlı olarak @NotNull değil: null göndermek "premium iptal et" anlamına gelir.
public record UpdatePremiumRequest(LocalDateTime premiumExpiresAt) {
}
