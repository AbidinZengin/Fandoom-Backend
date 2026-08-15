package com.example.fandoom_backend.lore.entity;

// CHARACTER + LOCATION destekleniyor (ör. "House Stark" hanesinin
// "Winterfell" lokasyonuna atanması). Event katılımcılığı (kişi/grup)
// bilinçli olarak buraya değil, ayrı EventParticipant tablosuna eklendi —
// aynı ilişkiyi iki yerden kurmamak için (bkz. lore tasarım dokümanı).
public enum TaggableType {
    CHARACTER, LOCATION
}
