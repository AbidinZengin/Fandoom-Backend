package com.example.fandoom_backend.movie.service;

import com.example.fandoom_backend.movie.dto.MovieSearchResult;
import com.example.fandoom_backend.movie.mapper.MovieMapper;
import com.example.fandoom_backend.movie.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieServiceImplQuickSearchTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private MovieMapper movieMapper;

    @InjectMocks
    private MovieServiceImpl movieService;

    @Test
    void quickSearch_buildsContainsAndPrefixPatterns_andLimitsToFirstPage() {
        MovieSearchResult dune = new MovieSearchResult(3L, "dune", "Dune", null, null, LocalDate.of(2021, 10, 22));
        when(movieRepository.quickSearch("%dune%", "dune%", PageRequest.of(0, 5))).thenReturn(List.of(dune));

        List<MovieSearchResult> result = movieService.quickSearch("dune", 5);

        assertThat(result).containsExactly(dune);
    }

    @Test
    void fullSearch_usesSameEscapedPatterns_andGivenPage() {
        when(movieRepository.fullSearch("%50!%%", "50!%%", PageRequest.of(1, 20))).thenReturn(Page.empty());

        movieService.fullSearch("50%", PageRequest.of(1, 20));

        verify(movieRepository).fullSearch("%50!%%", "50!%%", PageRequest.of(1, 20));
    }

    @Test
    void quickSearch_escapesUserWildcards() {
        movieService.quickSearch("50%", 5);

        verify(movieRepository).quickSearch("%50!%%", "50!%%", PageRequest.of(0, 5));
    }
}
