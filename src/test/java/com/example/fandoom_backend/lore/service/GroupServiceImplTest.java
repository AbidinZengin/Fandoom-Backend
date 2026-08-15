package com.example.fandoom_backend.lore.service;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.lore.dto.GroupRequest;
import com.example.fandoom_backend.lore.dto.GroupResponse;
import com.example.fandoom_backend.lore.entity.Group;
import com.example.fandoom_backend.lore.entity.SubjectType;
import com.example.fandoom_backend.lore.entity.TaxonomyCategory;
import com.example.fandoom_backend.lore.mapper.GroupMapper;
import com.example.fandoom_backend.lore.repository.GroupRepository;
import com.example.fandoom_backend.lore.repository.TaxonomyCategoryRepository;
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
class GroupServiceImplTest {

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private TaxonomyCategoryRepository taxonomyCategoryRepository;
    @Mock
    private GroupMapper groupMapper;
    @Mock
    private ImageStorageService imageStorageService;
    @Mock
    private MovieService movieService;
    @Mock
    private SeriesService seriesService;

    private GroupServiceImpl service;

    private final TaxonomyCategory housesCategory = TaxonomyCategory.builder()
            .id(1L).name("Haneler").slug("haneler").subjectType(SubjectType.SERIES).subjectId(10L).build();

    @BeforeEach
    void setUp() {
        service = new GroupServiceImpl(groupRepository, taxonomyCategoryRepository, groupMapper,
                imageStorageService, movieService, seriesService);

        lenient().when(groupRepository.existsBySlug(anyString())).thenReturn(false);
        lenient().when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(groupMapper.toResponse(any())).thenReturn(
                new GroupResponse(1L, "House Stark", "house-stark", null, 1L, "Haneler", "haneler",
                        SubjectType.SERIES, 10L, null));
    }

    @Test
    void addToSeries_categoryBelongsToSameSeries_savesGroup() {
        when(seriesService.existsById(10L)).thenReturn(true);
        when(taxonomyCategoryRepository.findById(1L)).thenReturn(Optional.of(housesCategory));
        GroupRequest request = new GroupRequest("House Stark", null, 1L, "{\"sigil\":\"Direwolf\"}");

        service.addToSeries(10L, request);

        ArgumentCaptor<Group> captor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(captor.capture());
        Group saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("House Stark");
        assertThat(saved.getSlug()).isEqualTo("house-stark");
        assertThat(saved.getCategory()).isEqualTo(housesCategory);
        assertThat(saved.getCustomFields()).isEqualTo("{\"sigil\":\"Direwolf\"}");
    }

    @Test
    void addToSeries_categoryBelongsToDifferentSeries_throwsInvalidReferenceException() {
        when(seriesService.existsById(20L)).thenReturn(true);
        when(taxonomyCategoryRepository.findById(1L)).thenReturn(Optional.of(housesCategory));
        GroupRequest request = new GroupRequest("House Stark", null, 1L, null);

        assertThatThrownBy(() -> service.addToSeries(20L, request))
                .isInstanceOf(InvalidReferenceException.class);

        verify(groupRepository, never()).save(any());
    }

    @Test
    void addToSeries_categoryDoesNotExist_throwsInvalidReferenceException() {
        when(seriesService.existsById(10L)).thenReturn(true);
        when(taxonomyCategoryRepository.findById(99L)).thenReturn(Optional.empty());
        GroupRequest request = new GroupRequest("House Stark", null, 99L, null);

        assertThatThrownBy(() -> service.addToSeries(10L, request))
                .isInstanceOf(InvalidReferenceException.class);

        verify(groupRepository, never()).save(any());
    }

    @Test
    void addToMovie_movieDoesNotExist_throwsResourceNotFoundException() {
        when(movieService.existsById(404L)).thenReturn(false);
        GroupRequest request = new GroupRequest("Dragon", null, 1L, null);

        assertThatThrownBy(() -> service.addToMovie(404L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(groupRepository, never()).save(any());
    }

    @Test
    void update_categoryStillBelongsToSameSubject_updatesFields() {
        Group group = Group.builder().id(1L).name("House Stark").slug("house-stark")
                .imageUrl("old.png").category(housesCategory).build();
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(taxonomyCategoryRepository.findById(1L)).thenReturn(Optional.of(housesCategory));
        GroupRequest request = new GroupRequest("House Stark", "new.png", 1L, "{\"words\":\"Winter is Coming\"}");

        service.update(1L, request);

        verify(imageStorageService).deleteIfChanged("old.png", "new.png");
        assertThat(group.getImageUrl()).isEqualTo("new.png");
        assertThat(group.getCustomFields()).isEqualTo("{\"words\":\"Winter is Coming\"}");
    }

    @Test
    void delete_existingGroup_deletesImageAndEntity() {
        Group group = Group.builder().id(5L).name("House Stark").slug("house-stark")
                .imageUrl("img.png").category(housesCategory).build();
        when(groupRepository.findById(5L)).thenReturn(Optional.of(group));

        service.delete(5L);

        verify(imageStorageService).delete("img.png");
        verify(groupRepository).deleteById(5L);
    }

    @Test
    void listForSeries_categoryIdNull_usesFindByCategory_SubjectTypeAndSubjectId() {
        service.listForSeries(10L, null);

        verify(groupRepository).findByCategory_SubjectTypeAndCategory_SubjectId(SubjectType.SERIES, 10L);
    }

    @Test
    void listForSeries_categoryIdGiven_usesFindByCategory_SubjectTypeAndSubjectIdAndCategoryId() {
        service.listForSeries(10L, 1L);

        verify(groupRepository).findByCategory_SubjectTypeAndCategory_SubjectIdAndCategoryId(
                SubjectType.SERIES, 10L, 1L);
    }
}
