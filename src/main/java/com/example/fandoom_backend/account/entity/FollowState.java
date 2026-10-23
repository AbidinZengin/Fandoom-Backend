package com.example.fandoom_backend.account.entity;

// NONE yalnızca API yanıtında anlam taşır (satır yok); DB'de FOLLOWING/REQUESTED saklanır.
public enum FollowState {
    NONE, FOLLOWING, REQUESTED
}
