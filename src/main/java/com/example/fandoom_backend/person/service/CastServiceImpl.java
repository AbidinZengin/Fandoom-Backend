package com.example.fandoom_backend.person.service;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.person.dto.CastRequest;
import com.example.fandoom_backend.person.dto.CastResponse;
import com.example.fandoom_backend.person.entity.Cast;
import com.example.fandoom_backend.person.entity.Character;
import com.example.fandoom_backend.person.entity.Person;
import com.example.fandoom_backend.person.entity.SubjectType;
import com.example.fandoom_backend.person.mapper.CastMapper;
import com.example.fandoom_backend.person.repository.CastRepository;
import com.example.fandoom_backend.person.repository.CharacterRepository;
import com.example.fandoom_backend.person.repository.PersonRepository;
import com.example.fandoom_backend.series.service.SeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CastServiceImpl implements CastService {

    private final CastRepository castRepository;
    private final PersonRepository personRepository;
    private final CharacterRepository characterRepository;
    private final CastMapper castMapper;
    private final MovieService movieService;
    private final SeriesService seriesService;

    @Override
    public List<CastResponse> listForMovie(Long movieId) {
        return castMapper.toResponseList(
                castRepository.findBySubjectTypeAndSubjectIdOrderByBillingOrderAsc(SubjectType.MOVIE, movieId));
    }

    @Override
    public List<CastResponse> listForSeries(Long seriesId) {
        return castMapper.toResponseList(
                castRepository.findBySubjectTypeAndSubjectIdOrderByBillingOrderAsc(SubjectType.SERIES, seriesId));
    }

    @Override
    @Transactional
    public CastResponse addToMovie(Long movieId, CastRequest request) {
        if (!movieService.existsById(movieId)) {
            throw new ResourceNotFoundException("Movie bulunamadı: id=" + movieId);
        }
        return castMapper.toResponse(castRepository.save(buildCast(SubjectType.MOVIE, movieId, request)));
    }

    @Override
    @Transactional
    public CastResponse addToSeries(Long seriesId, CastRequest request) {
        if (!seriesService.existsById(seriesId)) {
            throw new ResourceNotFoundException("Series bulunamadı: id=" + seriesId);
        }
        return castMapper.toResponse(castRepository.save(buildCast(SubjectType.SERIES, seriesId, request)));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!castRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cast kaydı bulunamadı: id=" + id);
        }
        castRepository.deleteById(id);
    }

    private Cast buildCast(SubjectType subjectType, Long subjectId, CastRequest request) {
        Person person = personRepository.findById(request.personId())
                .orElseThrow(() -> new InvalidReferenceException("Geçersiz person id: " + request.personId()));
        Character character = characterRepository.findById(request.characterId())
                .orElseThrow(() -> new InvalidReferenceException("Geçersiz character id: " + request.characterId()));
        return Cast.builder()
                .person(person)
                .character(character)
                .subjectType(subjectType)
                .subjectId(subjectId)
                .billingOrder(request.billingOrder())
                .build();
    }
}
