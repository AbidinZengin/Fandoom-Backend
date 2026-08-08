package com.example.fandoom_backend.group.service;

import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.group.dto.GroupRequest;
import com.example.fandoom_backend.group.dto.GroupResponse;
import com.example.fandoom_backend.group.entity.Group;
import com.example.fandoom_backend.group.entity.GroupType;
import com.example.fandoom_backend.group.entity.SubjectType;
import com.example.fandoom_backend.group.mapper.GroupMapper;
import com.example.fandoom_backend.group.repository.GroupRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceImplTest {

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMapper groupMapper;
    @Mock
    private ImageStorageService imageStorageService;
    @Mock
    private MovieService movieService;
    @Mock
    private SeriesService seriesService;

    private GroupServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GroupServiceImpl(groupRepository, groupMapper, imageStorageService, movieService, seriesService);

        lenient().when(groupRepository.existsBySlug(anyString())).thenReturn(false);
        lenient().when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(groupMapper.toResponse(any())).thenReturn(
                new GroupResponse(1L, "House Stark", "house-stark", null, GroupType.FACTION, SubjectType.MOVIE, 10L));
    }

    @Test
    void addToMovie_movieExists_savesGroupWithMovieSubjectType() {
        when(movieService.existsById(10L)).thenReturn(true);
        GroupRequest request = new GroupRequest("House Stark", null, GroupType.FACTION);

        service.addToMovie(10L, request);

        ArgumentCaptor<Group> captor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(captor.capture());
        Group saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("House Stark");
        assertThat(saved.getSlug()).isEqualTo("house-stark");
        assertThat(saved.getType()).isEqualTo(GroupType.FACTION);
        assertThat(saved.getSubjectType()).isEqualTo(SubjectType.MOVIE);
        assertThat(saved.getSubjectId()).isEqualTo(10L);
    }

    @Test
    void addToMovie_movieDoesNotExist_throwsResourceNotFoundException() {
        when(movieService.existsById(99L)).thenReturn(false);
        GroupRequest request = new GroupRequest("House Stark", null, GroupType.FACTION);

        assertThatThrownBy(() -> service.addToMovie(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(groupRepository, never()).save(any());
    }

    @Test
    void addToSeries_seriesExists_savesGroupWithSeriesSubjectType() {
        when(seriesService.existsById(20L)).thenReturn(true);
        GroupRequest request = new GroupRequest("Dragon", null, GroupType.SPECIES);

        service.addToSeries(20L, request);

        ArgumentCaptor<Group> captor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(captor.capture());
        Group saved = captor.getValue();
        assertThat(saved.getSubjectType()).isEqualTo(SubjectType.SERIES);
        assertThat(saved.getSubjectId()).isEqualTo(20L);
        assertThat(saved.getType()).isEqualTo(GroupType.SPECIES);
    }

    @Test
    void addToSeries_seriesDoesNotExist_throwsResourceNotFoundException() {
        when(seriesService.existsById(404L)).thenReturn(false);
        GroupRequest request = new GroupRequest("Dragon", null, GroupType.SPECIES);

        assertThatThrownBy(() -> service.addToSeries(404L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(groupRepository, never()).save(any());
    }

    @Test
    void update_nameUnchanged_keepsSlugAndDeletesOldImageIfChanged() {
        Group group = Group.builder().id(1L).name("House Stark").slug("house-stark")
                .imageUrl("old.png").type(GroupType.FACTION)
                .subjectType(SubjectType.MOVIE).subjectId(10L).build();
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        GroupRequest request = new GroupRequest("House Stark", "new.png", GroupType.FACTION);

        service.update(1L, request);

        verify(imageStorageService).deleteIfChanged("old.png", "new.png");
        verify(groupRepository, never()).existsBySlugAndIdNot(anyString(), eq(1L));
        assertThat(group.getSlug()).isEqualTo("house-stark");
        assertThat(group.getImageUrl()).isEqualTo("new.png");
    }

    @Test
    void update_nameChanged_regeneratesSlug() {
        Group group = Group.builder().id(1L).name("House Stark").slug("house-stark")
                .type(GroupType.FACTION).subjectType(SubjectType.MOVIE).subjectId(10L).build();
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupRepository.existsBySlugAndIdNot("house-targaryen", 1L)).thenReturn(false);
        GroupRequest request = new GroupRequest("House Targaryen", null, GroupType.FACTION);

        service.update(1L, request);

        assertThat(group.getSlug()).isEqualTo("house-targaryen");
        assertThat(group.getName()).isEqualTo("House Targaryen");
    }

    @Test
    void delete_existingGroup_deletesImageAndEntity() {
        Group group = Group.builder().id(5L).name("House Stark").slug("house-stark")
                .imageUrl("img.png").type(GroupType.FACTION)
                .subjectType(SubjectType.MOVIE).subjectId(10L).build();
        when(groupRepository.findById(5L)).thenReturn(Optional.of(group));

        service.delete(5L);

        verify(imageStorageService).delete("img.png");
        verify(groupRepository).deleteById(5L);
    }

    @Test
    void delete_notFound_throwsResourceNotFoundException() {
        when(groupRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listForMovie_typeNull_usesFindBySubjectTypeAndSubjectId() {
        service.listForMovie(10L, null);

        verify(groupRepository).findBySubjectTypeAndSubjectId(SubjectType.MOVIE, 10L);
        verify(groupRepository, never()).findBySubjectTypeAndSubjectIdAndType(any(), any(), any());
    }

    @Test
    void listForMovie_typeGiven_usesFindBySubjectTypeAndSubjectIdAndType() {
        service.listForMovie(10L, GroupType.SPECIES);

        verify(groupRepository).findBySubjectTypeAndSubjectIdAndType(SubjectType.MOVIE, 10L, GroupType.SPECIES);
        verify(groupRepository, never()).findBySubjectTypeAndSubjectId(any(), any());
    }

    @Test
    void getBySlug_notFound_throwsResourceNotFoundException() {
        when(groupRepository.findBySlug("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBySlug("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
