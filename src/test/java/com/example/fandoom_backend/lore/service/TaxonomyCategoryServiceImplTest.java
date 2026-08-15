package com.example.fandoom_backend.lore.service;

import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.lore.dto.TaxonomyCategoryRequest;
import com.example.fandoom_backend.lore.dto.TaxonomyCategoryResponse;
import com.example.fandoom_backend.lore.entity.SubjectType;
import com.example.fandoom_backend.lore.entity.TaxonomyCategory;
import com.example.fandoom_backend.lore.mapper.TaxonomyCategoryMapper;
import com.example.fandoom_backend.lore.repository.TaxonomyCategoryRepository;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.service.SeriesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxonomyCategoryServiceImplTest {

    @Mock
    private TaxonomyCategoryRepository taxonomyCategoryRepository;
    @Mock
    private TaxonomyCategoryMapper taxonomyCategoryMapper;
    @Mock
    private MovieService movieService;
    @Mock
    private SeriesService seriesService;

    private TaxonomyCategoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TaxonomyCategoryServiceImpl(taxonomyCategoryRepository, taxonomyCategoryMapper,
                movieService, seriesService);

        lenient().when(taxonomyCategoryRepository.existsBySlug(anyString())).thenReturn(false);
        lenient().when(taxonomyCategoryRepository.save(any(TaxonomyCategory.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(taxonomyCategoryMapper.toResponse(any())).thenReturn(
                new TaxonomyCategoryResponse(1L, "Haneler", "haneler", SubjectType.SERIES, 10L));
    }

    @Test
    void addToSeries_seriesExists_savesCategory() {
        when(seriesService.existsById(10L)).thenReturn(true);
        TaxonomyCategoryRequest request = new TaxonomyCategoryRequest("Haneler", null, null);

        service.addToSeries(10L, request);

        ArgumentCaptor<TaxonomyCategory> captor = ArgumentCaptor.forClass(TaxonomyCategory.class);
        verify(taxonomyCategoryRepository).save(captor.capture());
        TaxonomyCategory saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Haneler");
        assertThat(saved.getSlug()).isEqualTo("haneler");
        assertThat(saved.getSubjectType()).isEqualTo(SubjectType.SERIES);
        assertThat(saved.getSubjectId()).isEqualTo(10L);
    }

    @Test
    void addToSeries_seriesDoesNotExist_throwsResourceNotFoundException() {
        when(seriesService.existsById(404L)).thenReturn(false);
        TaxonomyCategoryRequest request = new TaxonomyCategoryRequest("Haneler", null, null);

        assertThatThrownBy(() -> service.addToSeries(404L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taxonomyCategoryRepository, never()).save(any());
    }

    @Test
    void addToMovie_movieDoesNotExist_throwsResourceNotFoundException() {
        when(movieService.existsById(404L)).thenReturn(false);
        TaxonomyCategoryRequest request = new TaxonomyCategoryRequest("Canavar Türleri", null, null);

        assertThatThrownBy(() -> service.addToMovie(404L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taxonomyCategoryRepository, never()).save(any());
    }

    @Test
    void update_nameChanged_regeneratesSlug() {
        TaxonomyCategory category = TaxonomyCategory.builder().id(1L).name("Haneler").slug("haneler")
                .subjectType(SubjectType.SERIES).subjectId(10L).build();
        when(taxonomyCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(taxonomyCategoryRepository.existsBySlugAndIdNot("klanlar", 1L)).thenReturn(false);

        service.update(1L, new TaxonomyCategoryRequest("Klanlar", null, null));

        assertThat(category.getSlug()).isEqualTo("klanlar");
        assertThat(category.getName()).isEqualTo("Klanlar");
    }

    @Test
    void delete_notFound_throwsResourceNotFoundException() {
        when(taxonomyCategoryRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(404L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taxonomyCategoryRepository, never()).deleteById(any());
    }
}
