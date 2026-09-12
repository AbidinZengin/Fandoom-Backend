package com.example.fandoom_backend.community.entity;

// Comment'in polimorfik olarak bağlanabildiği konu tipleri — person/Cast'teki
// SubjectType(MOVIE/SERIES) ile aynı desen (subjectType+subjectId, gerçek FK
// yok). THREAD community/'nin kendi aggregate'i (gerçek entity erişimi
// serbest); BLOG/SEASON/EPISODE cross-module, sadece ilgili service
// interface'i (existsById) ile doğrulanır.
public enum CommentSubjectType {
    THREAD, BLOG, SEASON, EPISODE
}
