package com.example.fandoom_backend.account.dto;

// DELETE /api/me/saved-items/{id} savedItemId ister (itemId değil), bu yüzden
// boolean tek başına yetmez — kayıtlıysa silme için gereken id de döner.
// "saved" burada itemType'ın VARSAYILAN sistem listesine (BLOG->READLIST,
// MOVIE|SERIES->WATCHLIST) göre değerlendirilir; öğe yalnızca bir CUSTOM
// listede kayıtlıysa bu false döner (generic Save butonu varsayılan listeyi
// temsil eder, CUSTOM liste üyeliğini değil).
public record SavedItemStatusResponse(boolean saved, Long savedItemId) {
}
