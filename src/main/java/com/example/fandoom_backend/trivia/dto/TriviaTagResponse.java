package com.example.fandoom_backend.trivia.dto;

// code = trivia `tag`/`tags[]` değeri; label istek diline göre çözülür (tr ise labelTr, boşsa EN'e düşer),
// labelEn/labelTr ham döner.
public record TriviaTagResponse(Long id, String code, String label, String labelEn, String labelTr) {
}
