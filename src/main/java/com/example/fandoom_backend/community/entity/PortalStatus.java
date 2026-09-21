package com.example.fandoom_backend.community.entity;

// ACTIVE: normal. ARCHIVED: listelenir/okunur ama yeni thread yazılamaz ve yeni üye alınmaz.
// HIDDEN: hiçbir public uçta görünmez (portal ve içindeki thread'ler 404), yalnız admin listesinde vardır.
public enum PortalStatus {
    ACTIVE, ARCHIVED, HIDDEN
}
