package com.example.fandoom_backend.user.entity;

public enum Role {
    USER, EDITOR, MODERATOR, ADMIN;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
