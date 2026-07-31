package com.example.fandoom_backend.blog.entity;

// person/'daki Cast.SubjectType ile aynı kavram, kasıtlı olarak ayrı kopya:
// modül bağımsızlığı kuralı gereği blog/ person/'ın entity paketine
// bağımlı olamaz.
public enum SubjectType {
    MOVIE, SERIES
}
