package com.example.fandoom_backend.lore.service;

import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.lore.dto.LocationRequest;
import com.example.fandoom_backend.lore.dto.LocationResponse;
import com.example.fandoom_backend.lore.entity.Location;
import com.example.fandoom_backend.lore.entity.SubjectType;
import com.example.fandoom_backend.lore.mapper.LocationMapper;
import com.example.fandoom_backend.lore.repository.LocationRepository;
import com.example.fandoom_backend.media.service.ImageStorageService;
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
class LocationServiceImplTest {

    @Mock
    private LocationRepository locationRepository;
    @Mock
    private LocationMapper locationMapper;
    @Mock
    private ImageStorageService imageStorageService;
    @Mock
    private MovieService movieService;
    @Mock
    private SeriesService seriesService;

    private LocationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LocationServiceImpl(locationRepository, locationMapper, imageStorageService,
                movieService, seriesService);

        lenient().when(locationRepository.existsBySlug(anyString())).thenReturn(false);
        lenient().when(locationRepository.save(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(locationMapper.toResponse(any())).thenReturn(
                new LocationResponse(1L, "Winterfell", "winterfell", null, null,
                        "{\"x\":0.42,\"y\":0.67}", SubjectType.SERIES, 10L));
    }

    @Test
    void addToSeries_seriesExists_savesLocation() {
        when(seriesService.existsById(10L)).thenReturn(true);
        LocationRequest request = new LocationRequest("Winterfell", null, "Starkların kalesi",
                "{\"x\":0.42,\"y\":0.67}");

        service.addToSeries(10L, request);

        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        verify(locationRepository).save(captor.capture());
        Location saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Winterfell");
        assertThat(saved.getDescription()).isEqualTo("Starkların kalesi");
        assertThat(saved.getCustomFields()).isEqualTo("{\"x\":0.42,\"y\":0.67}");
        assertThat(saved.getSubjectType()).isEqualTo(SubjectType.SERIES);
        assertThat(saved.getSubjectId()).isEqualTo(10L);
    }

    @Test
    void addToMovie_movieDoesNotExist_throwsResourceNotFoundException() {
        when(movieService.existsById(404L)).thenReturn(false);
        LocationRequest request = new LocationRequest("Somewhere", null, null, null);

        assertThatThrownBy(() -> service.addToMovie(404L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(locationRepository, never()).save(any());
    }

    @Test
    void delete_existingLocation_deletesImageAndEntity() {
        Location location = Location.builder().id(5L).name("Winterfell").slug("winterfell")
                .imageUrl("img.png").subjectType(SubjectType.SERIES).subjectId(10L).build();
        when(locationRepository.findById(5L)).thenReturn(Optional.of(location));

        service.delete(5L);

        verify(imageStorageService).delete("img.png");
        verify(locationRepository).deleteById(5L);
    }

    @Test
    void getBySlug_notFound_throwsResourceNotFoundException() {
        when(locationRepository.findBySlug("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBySlug("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
