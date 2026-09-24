package com.example.fandoom_backend.trivia.service;

import com.example.fandoom_backend.trivia.dto.TriviaRequest;
import com.example.fandoom_backend.trivia.dto.TriviaResponse;
import com.example.fandoom_backend.trivia.entity.TriviaItemType;

import java.util.List;

public interface TriviaService {

    /**
     * @param limit  null = hepsi; verilirse en fazla {@link TriviaServiceImpl#MAX_LIMIT}
     * @param random true ise sıralama DB seviyesinde rastgele, false ise yeniden eskiye
     */
    List<TriviaResponse> list(Long itemId, TriviaItemType itemType, Integer limit, boolean random);

    TriviaResponse create(TriviaRequest request, Long createdBy);

    TriviaResponse update(Long id, TriviaRequest request);

    void delete(Long id);
}
