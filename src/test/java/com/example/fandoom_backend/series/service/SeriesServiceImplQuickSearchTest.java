package com.example.fandoom_backend.series.service;

import com.example.fandoom_backend.series.dto.SeriesSearchResult;
import com.example.fandoom_backend.series.repository.SeriesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeriesServiceImplQuickSearchTest {

    @Mock
    private SeriesRepository seriesRepository;

    @InjectMocks
    private SeriesServiceImpl seriesService;

    @Test
    void quickSearch_buildsContainsAndPrefixPatterns_andLimitsToFirstPage() {
        SeriesSearchResult prophecy = new SeriesSearchResult(
                5L, "dune-prophecy", "Dune: Prophecy", null, null, LocalDate.of(2024, 11, 17));
        when(seriesRepository.quickSearch("%dune%", "dune%", PageRequest.of(0, 5))).thenReturn(List.of(prophecy));

        List<SeriesSearchResult> result = seriesService.quickSearch("dune", 5);

        assertThat(result).containsExactly(prophecy);
    }

    @Test
    void quickSearch_escapesUserWildcards() {
        seriesService.quickSearch("a_b", 5);

        verify(seriesRepository).quickSearch("%a!_b%", "a!_b%", PageRequest.of(0, 5));
    }
}
