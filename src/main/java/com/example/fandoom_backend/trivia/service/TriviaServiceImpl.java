package com.example.fandoom_backend.trivia.service;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.media.service.ImageStorageService;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.service.SeriesService;
import com.example.fandoom_backend.trivia.dto.TriviaRequest;
import com.example.fandoom_backend.trivia.dto.TriviaResponse;
import com.example.fandoom_backend.trivia.dto.TriviaTagResponse;
import com.example.fandoom_backend.trivia.entity.Trivia;
import com.example.fandoom_backend.trivia.entity.TriviaItemType;
import com.example.fandoom_backend.trivia.entity.TriviaTag;
import com.example.fandoom_backend.trivia.mapper.TriviaMapper;
import com.example.fandoom_backend.trivia.repository.TriviaRepository;
import com.example.fandoom_backend.trivia.repository.TriviaTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TriviaServiceImpl implements TriviaService {

    // Bir yapımın trivia sayısı küçük; yine de "limit yok" isteği sınırsız satır çekmesin.
    static final int MAX_LIMIT = 100;
    static final int MAX_TAGS = 5;

    private final TriviaRepository triviaRepository;
    private final TriviaMapper triviaMapper;
    private final MovieService movieService;
    private final SeriesService seriesService;
    private final ImageStorageService imageStorageService;
    private final TriviaTagRepository triviaTagRepository;

    @Override
    public List<TriviaResponse> list(Long itemId, TriviaItemType itemType, Integer limit, boolean random) {
        if (limit != null && limit < 1) {
            throw new InvalidReferenceException("limit en az 1 olmalı");
        }
        int effectiveLimit = limit == null ? MAX_LIMIT : Math.min(limit, MAX_LIMIT);
        List<Trivia> result;
        if (random) {
            result = triviaRepository.findRandom(itemId, itemType.name(), effectiveLimit);
        } else {
            Pageable pageable = PageRequest.of(0, effectiveLimit,
                    Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id")));
            result = triviaRepository.findByItemIdAndItemType(itemId, itemType, pageable);
        }
        return triviaMapper.toResponseList(result);
    }

    @Override
    @Transactional
    public TriviaResponse create(TriviaRequest request, Long createdBy) {
        assertItemExists(request.itemType(), request.itemId());
        Trivia trivia = Trivia.builder()
                .itemId(request.itemId())
                .itemType(request.itemType())
                .title(blankToNull(request.title()))
                .titleTr(blankToNull(request.titleTr()))
                .content(request.content().trim())
                .contentTr(blankToNull(request.contentTr()))
                .imageUrl(blankToNull(request.imageUrl()))
                .tags(resolveTags(request))
                .spoiler(Boolean.TRUE.equals(request.isSpoiler()))
                .sourceUrl(request.sourceUrl())
                .createdBy(createdBy)
                .build();
        return triviaMapper.toResponse(triviaRepository.save(trivia));
    }

    @Override
    @Transactional
    public TriviaResponse update(Long id, TriviaRequest request) {
        Trivia trivia = triviaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trivia bulunamadı: id=" + id));
        if (!trivia.getItemId().equals(request.itemId()) || trivia.getItemType() != request.itemType()) {
            assertItemExists(request.itemType(), request.itemId());
            trivia.setItemId(request.itemId());
            trivia.setItemType(request.itemType());
        }
        String newImageUrl = blankToNull(request.imageUrl());
        imageStorageService.deleteIfChanged(trivia.getImageUrl(), newImageUrl);
        trivia.setTitle(blankToNull(request.title()));
        trivia.setTitleTr(blankToNull(request.titleTr()));
        trivia.setContent(request.content().trim());
        trivia.setContentTr(blankToNull(request.contentTr()));
        trivia.setImageUrl(newImageUrl);
        trivia.getTags().clear();
        trivia.getTags().addAll(resolveTags(request));
        trivia.setSpoiler(Boolean.TRUE.equals(request.isSpoiler()));
        trivia.setSourceUrl(request.sourceUrl());
        return triviaMapper.toResponse(trivia);
    }

    @Override
    public List<TriviaTagResponse> listTags() {
        return triviaMapper.toTagResponseList(triviaTagRepository.findAllByOrderByIdAsc());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Trivia trivia = triviaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trivia bulunamadı: id=" + id));
        imageStorageService.delete(trivia.getImageUrl());
        triviaRepository.delete(trivia);
    }

    // tag + tags birleşir (tag önce); her biri trivia_tag tablosundaki koda çözülür (büyük/küçük harf, boşluk
    // ve tire fark etmez: "behind the scenes" → BEHIND_THE_SCENES). Tabloda olmayan etiket 400 (kapalı küme).
    // Aynı etiket iki kez gelirse tekilleştirilir; sonuç kanonik kodlardır.
    private List<String> resolveTags(TriviaRequest request) {
        List<String> raw = new ArrayList<>();
        raw.add(request.tag());
        if (request.tags() != null) {
            raw.addAll(request.tags());
        }
        Map<String, String> unique = new LinkedHashMap<>();
        for (String value : raw) {
            String key = toLookupKey(value);
            if (key == null) {
                continue;
            }
            TriviaTag known = triviaTagRepository.findByCodeIgnoreCase(key)
                    .orElseThrow(() -> new InvalidReferenceException("Geçersiz tag: " + value.trim()));
            unique.putIfAbsent(known.getCode().toLowerCase(Locale.ROOT), known.getCode());
        }
        if (unique.isEmpty()) {
            throw new InvalidReferenceException("En az bir tag gerekli");
        }
        if (unique.size() > MAX_TAGS) {
            throw new InvalidReferenceException("En fazla " + MAX_TAGS + " tag eklenebilir");
        }
        return new ArrayList<>(unique.values());
    }

    // Boşlukları sadeleştirir, boşluk/tireyi alt çizgiye çevirir (kod biçimi); boşsa null.
    private static String toLookupKey(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.trim().replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) {
            return null;
        }
        return cleaned.replace(' ', '_').replace('-', '_');
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private void assertItemExists(TriviaItemType itemType, Long itemId) {
        boolean exists = switch (itemType) {
            case MOVIE -> movieService.existsById(itemId);
            case SERIES -> seriesService.existsById(itemId);
        };
        if (!exists) {
            throw new InvalidReferenceException("Geçersiz " + itemType.toValue() + " id: " + itemId);
        }
    }
}
