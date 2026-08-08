package com.example.fandoom_backend.person.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.common.util.SlugGenerator;
import com.example.fandoom_backend.person.dto.CharacterRequest;
import com.example.fandoom_backend.person.dto.CharacterResponse;
import com.example.fandoom_backend.person.entity.Character;
import com.example.fandoom_backend.person.entity.SubjectType;
import com.example.fandoom_backend.person.mapper.CharacterMapper;
import com.example.fandoom_backend.person.repository.CharacterRepository;
import com.example.fandoom_backend.media.service.ImageStorageService;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.service.SeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterServiceImpl implements CharacterService {

    private final CharacterRepository characterRepository;
    private final CharacterMapper characterMapper;
    private final ImageStorageService imageStorageService;
    private final MovieService movieService;
    private final SeriesService seriesService;

    @Override
    public PageResponse<CharacterResponse> list(Pageable pageable) {
        Page<CharacterResponse> page = characterRepository.findAll(pageable).map(characterMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    public List<CharacterResponse> listForMovie(Long movieId) {
        return characterMapper.toResponseList(
                characterRepository.findBySubjectTypeAndSubjectIdOrderByBillingOrderAsc(SubjectType.MOVIE, movieId));
    }

    @Override
    public List<CharacterResponse> listForSeries(Long seriesId) {
        return characterMapper.toResponseList(
                characterRepository.findBySubjectTypeAndSubjectIdOrderByBillingOrderAsc(SubjectType.SERIES, seriesId));
    }

    @Override
    public CharacterResponse getById(Long id) {
        return characterMapper.toResponse(findEntityById(id));
    }

    @Override
    public CharacterResponse getBySlug(String slug) {
        Character character = characterRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Character bulunamadı: slug=" + slug));
        return characterMapper.toResponse(character);
    }

    @Override
    @Transactional
    public CharacterResponse addToMovie(Long movieId, CharacterRequest request) {
        if (!movieService.existsById(movieId)) {
            throw new ResourceNotFoundException("Movie bulunamadı: id=" + movieId);
        }
        return characterMapper.toResponse(characterRepository.save(buildCharacter(SubjectType.MOVIE, movieId, request)));
    }

    @Override
    @Transactional
    public List<CharacterResponse> addToMovieBatch(Long movieId, List<CharacterRequest> requests) {
        return requests.stream()
                .map(request -> addToMovie(movieId, request))
                .toList();
    }

    @Override
    @Transactional
    public CharacterResponse addToSeries(Long seriesId, CharacterRequest request) {
        if (!seriesService.existsById(seriesId)) {
            throw new ResourceNotFoundException("Series bulunamadı: id=" + seriesId);
        }
        return characterMapper.toResponse(characterRepository.save(buildCharacter(SubjectType.SERIES, seriesId, request)));
    }

    @Override
    @Transactional
    public List<CharacterResponse> addToSeriesBatch(Long seriesId, List<CharacterRequest> requests) {
        return requests.stream()
                .map(request -> addToSeries(seriesId, request))
                .toList();
    }

    @Override
    @Transactional
    public CharacterResponse update(Long id, CharacterRequest request) {
        Character character = findEntityById(id);
        imageStorageService.deleteIfChanged(character.getImageUrl(), request.imageUrl());
        if (!character.getName().equals(request.name())) {
            character.setSlug(SlugGenerator.generateUnique(request.name(),
                    slug -> characterRepository.existsBySlugAndIdNot(slug, id)));
        }
        character.setName(request.name());
        character.setDescription(request.description());
        character.setQuote(request.quote());
        character.setImageUrl(request.imageUrl());
        character.setBillingOrder(request.billingOrder());
        return characterMapper.toResponse(character);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Character character = findEntityById(id);
        imageStorageService.delete(character.getImageUrl());
        characterRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return characterRepository.existsById(id);
    }

    private Character buildCharacter(SubjectType subjectType, Long subjectId, CharacterRequest request) {
        return Character.builder()
                .name(request.name())
                .slug(SlugGenerator.generateUnique(request.name(), characterRepository::existsBySlug))
                .description(request.description())
                .quote(request.quote())
                .imageUrl(request.imageUrl())
                .subjectType(subjectType)
                .subjectId(subjectId)
                .billingOrder(request.billingOrder())
                .build();
    }

    private Character findEntityById(Long id) {
        return characterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Character bulunamadı: id=" + id));
    }
}
