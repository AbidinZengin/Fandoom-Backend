package com.example.fandoom_backend.account.dto;

import com.example.fandoom_backend.common.validation.OnCreate;
import com.example.fandoom_backend.common.validation.OnUpdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// POST /api/me/lists'te OnCreate grubuyla tam doğrulanır (title zorunlu).
// PATCH /api/me/lists/{id}'de aynı record OnUpdate grubuyla — null alan
// "değişmedi" anlamına gelir (bkz. AccountController, UserListServiceImpl.update).
// title için OnUpdate'te @NotBlank yerine @Pattern(".*\S.*") kullanılır: @Pattern
// null'ı (alan hiç gönderilmedi) geçerli sayar ama gönderilip boş/boşluk
// bırakılmasını reddeder — @NotBlank null'ı da reddederdi. listType/isPinned
// burada YOK: listType her zaman CUSTOM açılır, isPinned ayrı PATCH .../pin
// ucundan yönetilir.
public record CreateUserListRequest(
        @NotBlank(groups = OnCreate.class, message = "boş olamaz")
        @Pattern(regexp = ".*\\S.*", groups = OnUpdate.class, message = "boş olamaz")
        @Size(max = 150, groups = {OnCreate.class, OnUpdate.class}, message = "en fazla 150 karakter olmalı")
        String title,
        @Size(max = 2000, groups = {OnCreate.class, OnUpdate.class}, message = "en fazla 2000 karakter olmalı")
        String description,
        @Size(max = 500, groups = {OnCreate.class, OnUpdate.class}, message = "en fazla 500 karakter olmalı")
        String coverImageUrl,
        Boolean isPublic) {
}
