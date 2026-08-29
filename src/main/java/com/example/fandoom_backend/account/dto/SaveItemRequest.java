package com.example.fandoom_backend.account.dto;

import com.example.fandoom_backend.account.entity.ListType;
import com.example.fandoom_backend.account.entity.SavedItemType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// targetListId/listType ikisi de null ise itemType'a göre sistem listesi
// otomatik çözülür (BLOG->READLIST, MOVIE|SERIES->WATCHLIST). targetListId
// verilirse (CUSTOM liste dahil) doğrudan o liste kullanılır. listType
// verilirse (yalnızca WATCHLIST/READLIST/WATCHED — CUSTOM burada geçersizdir,
// CUSTOM için targetListId kullanılır) o sistem listesi hedeflenir — "İzledim"
// gibi varsayılan-olmayan bir sistem listesine eklerken kullanılır. İkisi
// birden verilirse targetListId önceliklidir. Bkz. UserSavedItemServiceImpl.
public record SaveItemRequest(
        @NotNull Long itemId,
        @NotNull SavedItemType itemType,
        Long targetListId,
        ListType listType,
        @Size(max = 500) String notes) {
}
