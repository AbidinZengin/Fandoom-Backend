package com.example.fandoom_backend.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// POST /api/me/lists'te @Valid ile tam doğrulanır (title zorunlu).
// PATCH /api/me/lists/{id}'de aynı record, @Valid OLMADAN kısmi güncelleme
// için yeniden kullanılır — null alan "değişmedi" anlamına gelir
// (bkz. UserListServiceImpl.update). listType/isPinned burada YOK: listType
// her zaman CUSTOM açılır, isPinned ayrı PATCH .../pin ucundan yönetilir.
public record CreateUserListRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 2000) String description,
        @Size(max = 500) String coverImageUrl,
        Boolean isPublic) {
}
