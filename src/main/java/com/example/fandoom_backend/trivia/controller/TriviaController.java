package com.example.fandoom_backend.trivia.controller;

import com.example.fandoom_backend.trivia.dto.TriviaListResponse;
import com.example.fandoom_backend.trivia.dto.TriviaRequest;
import com.example.fandoom_backend.trivia.dto.TriviaResponse;
import com.example.fandoom_backend.trivia.entity.TriviaItemType;
import com.example.fandoom_backend.trivia.service.TriviaService;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trivia")
@RequiredArgsConstructor
public class TriviaController {

    private final TriviaService triviaService;

    @GetMapping
    public TriviaListResponse list(
            @RequestParam Long itemId,
            @RequestParam TriviaItemType itemType,
            @RequestParam(required = false) Integer limit,
            @RequestParam(defaultValue = "false") boolean random) {
        return new TriviaListResponse(triviaService.list(itemId, itemType, limit, random));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TriviaResponse create(@AuthenticationPrincipal CustomUserDetails principal,
                                 @Valid @RequestBody TriviaRequest request) {
        return triviaService.create(request, principal.getId());
    }

    @PutMapping("/{id}")
    public TriviaResponse update(@PathVariable Long id, @Valid @RequestBody TriviaRequest request) {
        return triviaService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        triviaService.delete(id);
    }
}
