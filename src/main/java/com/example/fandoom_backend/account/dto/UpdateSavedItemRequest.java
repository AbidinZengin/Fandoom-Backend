package com.example.fandoom_backend.account.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

// progressPercentage yalnızca BLOG için anlamlı; MOVIE/SERIES için gönderilse
// bile reddedilmez, salt bilgi amaçlı saklanır (bkz. tasarım dokümanı).
public record UpdateSavedItemRequest(
        @Min(0) @Max(100) Integer progressPercentage,
        @Size(max = 500) String notes) {
}
