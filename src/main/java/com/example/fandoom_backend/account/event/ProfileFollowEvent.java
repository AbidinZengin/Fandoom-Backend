package com.example.fandoom_backend.account.event;

// Kişi takibindeki değişiklikler. ProfileFollowServiceImpl yayınlar, notification/ dinler:
// account/ notification/'a bağımlı olmaz (bağımlılık tek yönlü: notification/ -> account/).
// Aynı transaction içinde işlenir (@EventListener), takip ve bildirim birlikte commit/rollback olur.
//
// actorId: eylemi yapan, recipientId: etkilenen/bildirim alacak taraf.
//   FOLLOWED          actor açık profildeki recipient'ı takip etti          -> recipient'a FOLLOW
//   REQUESTED         actor gizli profildeki recipient'a istek attı         -> recipient'a FOLLOW_REQUEST
//   ACCEPTED          actor (profil sahibi) recipient'ın isteğini onayladı  -> recipient'a FOLLOW_ACCEPTED
//   WITHDRAWN         actor takibi bıraktı / isteği geri çekti              -> recipient'taki okunmamış FOLLOW/FOLLOW_REQUEST silinir
//   REQUEST_REJECTED  actor (profil sahibi) recipient'ın isteğini reddetti  -> actor'daki bayat FOLLOW_REQUEST silinir
//                     (reddetme bildirim ÜRETMEZ)
public record ProfileFollowEvent(Type type, Long actorId, Long recipientId) {

    public enum Type {
        FOLLOWED, REQUESTED, ACCEPTED, WITHDRAWN, REQUEST_REJECTED
    }
}
