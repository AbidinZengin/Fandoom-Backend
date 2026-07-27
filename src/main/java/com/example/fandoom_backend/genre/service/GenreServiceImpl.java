package com.example.fandoom_backend.genre.service;

import com.example.fandoom_backend.common.exception.DuplicateResourceException;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.common.util.SlugGenerator;
import com.example.fandoom_backend.genre.dto.GenreRequest;
import com.example.fandoom_backend.genre.dto.GenreResponse;
import com.example.fandoom_backend.genre.entity.Genre;
import com.example.fandoom_backend.genre.mapper.GenreMapper;
import com.example.fandoom_backend.genre.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;
    private final GenreMapper genreMapper;

    @Override
    public List<GenreResponse> list() {
        return genreMapper.toResponseList(genreRepository.findAll());
    }

    @Override
    public GenreResponse getById(Long id) {
        return genreMapper.toResponse(findEntityById(id));
    }

    @Override
    @Transactional
    public GenreResponse create(GenreRequest request) {
        if (genreRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Bu isimde bir genre zaten var: " + request.name());
        }
        Genre genre = Genre.builder()
                .name(request.name())
                .slug(SlugGenerator.generateUnique(request.name(), genreRepository::existsBySlug))
                .build();
        return genreMapper.toResponse(genreRepository.save(genre));
    }

    @Override
    @Transactional
    public List<GenreResponse> createBatch(List<GenreRequest> requests) {
        return requests.stream()
                .map(this::create)
                .toList();
    }

    @Override
    @Transactional
    public GenreResponse update(Long id, GenreRequest request) {
        Genre genre = findEntityById(id);
        if (!genre.getName().equals(request.name())) {
            if (genreRepository.existsByNameAndIdNot(request.name(), id)) {
                throw new DuplicateResourceException("Bu isimde bir genre zaten var: " + request.name());
            }
            genre.setSlug(SlugGenerator.generateUnique(request.name(),
                    slug -> genreRepository.existsBySlugAndIdNot(slug, id)));
        }
        genre.setName(request.name());
        return genreMapper.toResponse(genre);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!genreRepository.existsById(id)) {
            throw new ResourceNotFoundException("Genre bulunamadı: id=" + id);
        }
        genreRepository.deleteById(id);
    }

    @Override
    public void assertAllExist(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Set<Long> existingIds = genreRepository.findAllById(ids).stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());
        Set<Long> missing = new HashSet<>(ids);
        missing.removeAll(existingIds);
        if (!missing.isEmpty()) {
            throw new InvalidReferenceException("Geçersiz genre id(leri): " + missing);
        }
    }

    private Genre findEntityById(Long id) {
        return genreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Genre bulunamadı: id=" + id));
    }
}
