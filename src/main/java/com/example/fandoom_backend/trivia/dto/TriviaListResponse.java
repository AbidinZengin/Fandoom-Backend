package com.example.fandoom_backend.trivia.dto;

import java.util.List;

// API sözleşmesi: liste {"data": [...]} zarfıyla döner.
public record TriviaListResponse(List<TriviaResponse> data) {
}
