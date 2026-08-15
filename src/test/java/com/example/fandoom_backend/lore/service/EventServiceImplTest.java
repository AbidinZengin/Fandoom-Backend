package com.example.fandoom_backend.lore.service;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.lore.dto.EventRequest;
import com.example.fandoom_backend.lore.dto.EventResponse;
import com.example.fandoom_backend.lore.entity.Event;
import com.example.fandoom_backend.lore.entity.Location;
import com.example.fandoom_backend.lore.entity.SubjectType;
import com.example.fandoom_backend.lore.mapper.EventMapper;
import com.example.fandoom_backend.lore.repository.EventRepository;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private EventMapper eventMapper;
    @Mock
    private ImageStorageService imageStorageService;
    @Mock
    private MovieService movieService;
    @Mock
    private SeriesService seriesService;

    private EventServiceImpl service;

    private final Location winterfell = Location.builder().id(1L).name("Winterfell").slug("winterfell")
            .subjectType(SubjectType.SERIES).subjectId(10L).build();

    @BeforeEach
    void setUp() {
        service = new EventServiceImpl(eventRepository, locationRepository, eventMapper,
                imageStorageService, movieService, seriesService);

        lenient().when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(eventMapper.toResponse(any())).thenReturn(
                new EventResponse(1L, "Red Wedding", "...", 1, null, SubjectType.SERIES, 10L, 1L, "Winterfell",
                        "winterfell", false, null));
    }

    @Test
    void addToSeries_locationBelongsToSameSeries_savesEvent() {
        when(seriesService.existsById(10L)).thenReturn(true);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(winterfell));
        EventRequest request = new EventRequest("Red Wedding", "...", 1, null, 1L, true,
                "{\"date\":\"ca. 300 AC\",\"quote\":\"...\"}");

        service.addToSeries(10L, request);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        Event saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Red Wedding");
        assertThat(saved.getOrderIndex()).isEqualTo(1);
        assertThat(saved.getLocation()).isEqualTo(winterfell);
        assertThat(saved.isPinned()).isTrue();
        assertThat(saved.getCustomFields()).isEqualTo("{\"date\":\"ca. 300 AC\",\"quote\":\"...\"}");
    }

    @Test
    void addToSeries_locationBelongsToDifferentSeries_throwsInvalidReferenceException() {
        when(seriesService.existsById(20L)).thenReturn(true);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(winterfell));
        EventRequest request = new EventRequest("Red Wedding", "...", 1, null, 1L, false, null);

        assertThatThrownBy(() -> service.addToSeries(20L, request))
                .isInstanceOf(InvalidReferenceException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void addToSeries_noLocation_savesEventWithoutLocation() {
        when(seriesService.existsById(10L)).thenReturn(true);
        EventRequest request = new EventRequest("Red Wedding", "...", 1, null, null, false, null);

        service.addToSeries(10L, request);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getLocation()).isNull();
        verify(locationRepository, never()).findById(any());
    }

    @Test
    void addToMovie_movieDoesNotExist_throwsResourceNotFoundException() {
        when(movieService.existsById(404L)).thenReturn(false);
        EventRequest request = new EventRequest("Some Event", null, 1, null, null, false, null);

        assertThatThrownBy(() -> service.addToMovie(404L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void delete_existingEvent_deletesImageAndEntity() {
        Event event = Event.builder().id(5L).name("Red Wedding").orderIndex(1).imageUrl("img.png")
                .subjectType(SubjectType.SERIES).subjectId(10L).build();
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));

        service.delete(5L);

        verify(imageStorageService).delete("img.png");
        verify(eventRepository).deleteById(5L);
    }

    @Test
    void listForSeries_ordersByOrderIndex() {
        service.listForSeries(10L);

        verify(eventRepository).findBySubjectTypeAndSubjectIdOrderByOrderIndexAsc(SubjectType.SERIES, 10L);
    }
}
