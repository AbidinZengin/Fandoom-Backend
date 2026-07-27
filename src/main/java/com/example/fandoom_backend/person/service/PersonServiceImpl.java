package com.example.fandoom_backend.person.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.common.util.SlugGenerator;
import com.example.fandoom_backend.person.dto.PersonDetailResponse;
import com.example.fandoom_backend.person.dto.PersonRequest;
import com.example.fandoom_backend.person.dto.PersonSummaryResponse;
import com.example.fandoom_backend.person.entity.Person;
import com.example.fandoom_backend.person.mapper.PersonMapper;
import com.example.fandoom_backend.person.repository.PersonRepository;
import com.example.fandoom_backend.media.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;
    private final ImageStorageService imageStorageService;

    @Override
    public PageResponse<PersonSummaryResponse> list(Pageable pageable) {
        Page<PersonSummaryResponse> page = personRepository.findAll(pageable).map(personMapper::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Override
    public PersonDetailResponse getById(Long id) {
        return personMapper.toDetailResponse(findEntityById(id));
    }

    @Override
    public PersonDetailResponse getBySlug(String slug) {
        Person person = personRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Person bulunamadı: slug=" + slug));
        return personMapper.toDetailResponse(person);
    }

    @Override
    @Transactional
    public PersonDetailResponse create(PersonRequest request) {
        Person person = Person.builder()
                .name(request.name())
                .slug(SlugGenerator.generateUnique(request.name(), personRepository::existsBySlug))
                .bio(request.bio())
                .photoUrl(request.photoUrl())
                .birthDate(request.birthDate())
                .build();
        return personMapper.toDetailResponse(personRepository.save(person));
    }

    @Override
    @Transactional
    public List<PersonDetailResponse> createBatch(List<PersonRequest> requests) {
        return requests.stream()
                .map(this::create)
                .toList();
    }

    @Override
    @Transactional
    public PersonDetailResponse update(Long id, PersonRequest request) {
        Person person = findEntityById(id);
        imageStorageService.deleteIfChanged(person.getPhotoUrl(), request.photoUrl());
        if (!person.getName().equals(request.name())) {
            person.setSlug(SlugGenerator.generateUnique(request.name(),
                    slug -> personRepository.existsBySlugAndIdNot(slug, id)));
        }
        person.setName(request.name());
        person.setBio(request.bio());
        person.setPhotoUrl(request.photoUrl());
        person.setBirthDate(request.birthDate());
        return personMapper.toDetailResponse(person);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Person person = findEntityById(id);
        imageStorageService.delete(person.getPhotoUrl());
        personRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return personRepository.existsById(id);
    }

    @Override
    public void assertAllExist(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Set<Long> existingIds = personRepository.findAllById(ids).stream()
                .map(Person::getId)
                .collect(Collectors.toSet());
        Set<Long> missing = new HashSet<>(ids);
        missing.removeAll(existingIds);
        if (!missing.isEmpty()) {
            throw new InvalidReferenceException("Geçersiz person id(leri): " + missing);
        }
    }

    private Person findEntityById(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Person bulunamadı: id=" + id));
    }
}
