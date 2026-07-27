package com.example.fandoom_backend.person.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.common.util.SlugGenerator;
import com.example.fandoom_backend.person.dto.CharacterRequest;
import com.example.fandoom_backend.person.dto.CharacterResponse;
import com.example.fandoom_backend.person.entity.Character;
import com.example.fandoom_backend.person.mapper.CharacterMapper;
import com.example.fandoom_backend.person.repository.CharacterRepository;
import com.example.fandoom_backend.media.service.ImageStorageService;
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

    @Override
    public PageResponse<CharacterResponse> list(Pageable pageable) {
        Page<CharacterResponse> page = characterRepository.findAll(pageable).map(characterMapper::toResponse);
        return PageResponse.from(page);
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
    public CharacterResponse create(CharacterRequest request) {
        Character character = Character.builder()
                .name(request.name())
                .slug(SlugGenerator.generateUnique(request.name(), characterRepository::existsBySlug))
                .description(request.description())
                .imageUrl(request.imageUrl())
                .build();
        return characterMapper.toResponse(characterRepository.save(character));
    }

    @Override
    @Transactional
    public List<CharacterResponse> createBatch(List<CharacterRequest> requests) {
        return requests.stream()
                .map(this::create)
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
        character.setImageUrl(request.imageUrl());
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

    private Character findEntityById(Long id) {
        return characterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Character bulunamadı: id=" + id));
    }
}
