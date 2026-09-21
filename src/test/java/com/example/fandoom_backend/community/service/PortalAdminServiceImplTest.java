package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.DuplicateResourceException;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.dto.AdminPortalResponse;
import com.example.fandoom_backend.community.dto.PortalCreateRequest;
import com.example.fandoom_backend.community.dto.PortalUpdateRequest;
import com.example.fandoom_backend.community.entity.Portal;
import com.example.fandoom_backend.community.entity.PortalPostingPolicy;
import com.example.fandoom_backend.community.entity.PortalProduction;
import com.example.fandoom_backend.community.entity.PortalProductionType;
import com.example.fandoom_backend.community.entity.PortalStatus;
import com.example.fandoom_backend.community.mapper.PortalMapperImpl;
import com.example.fandoom_backend.community.repository.PortalProductionRepository;
import com.example.fandoom_backend.community.repository.PortalRepository;
import com.example.fandoom_backend.media.service.MediaUrlValidator;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.service.SeriesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalAdminServiceImplTest {

    private static final String IMG = "https://res.cloudinary.com/c/image/upload/v1/fandoom/portals/a.webp";

    @Mock private PortalRepository portalRepository;
    @Mock private PortalProductionRepository portalProductionRepository;
    @Mock private MovieService movieService;
    @Mock private SeriesService seriesService;
    @Mock private MediaUrlValidator mediaUrlValidator;

    private PortalAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PortalAdminServiceImpl(portalRepository, portalProductionRepository, new PortalMapperImpl(),
                movieService, seriesService, mediaUrlValidator);
        lenient().when(portalRepository.save(any(Portal.class))).thenAnswer(inv -> {
            Portal p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(1L);
            }
            return p;
        });
        lenient().when(portalProductionRepository.findByPortalIdIn(any())).thenReturn(List.of());
        lenient().when(portalProductionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static PortalCreateRequest create(String slug, List<String> productions) {
        return new PortalCreateRequest(slug, "Ad", "Name", null, null, null, null, null, null, productions, null, null);
    }

    private static PortalUpdateRequest emptyUpdate() {
        return new PortalUpdateRequest(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private Portal existing() {
        return Portal.builder().id(1L).slug("westeros").nameTr("Westeros").nameEn("Westeros")
                .status(PortalStatus.ACTIVE).postingPolicy(PortalPostingPolicy.OPEN).build();
    }

    // ---- create ----

    @Test
    void create_appliesDefaults_andReturnsAdminView() {
        when(portalRepository.existsBySlug("yeni")).thenReturn(false);

        AdminPortalResponse response = service.create(create("yeni", null));

        assertThat(response.slug()).isEqualTo("yeni");
        assertThat(response.status()).isEqualTo(PortalStatus.ACTIVE);
        assertThat(response.postingPolicy()).isEqualTo(PortalPostingPolicy.OPEN);
        assertThat(response.sortOrder()).isZero();
        assertThat(response.memberCount()).isZero();
        assertThat(response.threadCount()).isZero();
        assertThat(response.nameTr()).isEqualTo("Ad");
        assertThat(response.nameEn()).isEqualTo("Name");
    }

    @Test
    void create_duplicateSlug_is409() {
        when(portalRepository.existsBySlug("var")).thenReturn(true);

        assertThatThrownBy(() -> service.create(create("var", null))).isInstanceOf(DuplicateResourceException.class);

        verify(portalRepository, never()).save(any());
    }

    @Test
    void create_linksProductions_movieWinsWhenBothExist_seriesOtherwise() {
        when(portalRepository.existsBySlug(anyString())).thenReturn(false);
        when(movieService.existsBySlug("film")).thenReturn(true);
        when(movieService.existsBySlug("dizi")).thenReturn(false);
        when(seriesService.existsBySlug("dizi")).thenReturn(true);
        when(portalProductionRepository.findByProductionSlug(anyString())).thenReturn(Optional.empty());

        service.create(create("westeros", List.of("film", "dizi", "film")));

        ArgumentCaptor<List<PortalProduction>> saved = ArgumentCaptor.forClass(List.class);
        verify(portalProductionRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(PortalProduction::getProductionSlug).containsExactly("film", "dizi");
        assertThat(saved.getValue()).extracting(PortalProduction::getProductionType)
                .containsExactly(PortalProductionType.MOVIE, PortalProductionType.SERIES);
    }

    @Test
    void create_unknownProduction_is400_andNothingSaved() {
        when(portalRepository.existsBySlug(anyString())).thenReturn(false);
        when(movieService.existsBySlug("yok")).thenReturn(false);
        when(seriesService.existsBySlug("yok")).thenReturn(false);

        assertThatThrownBy(() -> service.create(create("x-portal", List.of("yok"))))
                .isInstanceOf(InvalidReferenceException.class);

        verify(portalRepository, never()).save(any());
    }

    @Test
    void create_productionAlreadyLinkedToAnotherPortal_is409() {
        when(portalRepository.existsBySlug(anyString())).thenReturn(false);
        when(movieService.existsBySlug("got")).thenReturn(false);
        when(seriesService.existsBySlug("got")).thenReturn(true);
        when(portalProductionRepository.findByProductionSlug("got")).thenReturn(Optional.of(
                PortalProduction.builder().portalId(99L).productionSlug("got")
                        .productionType(PortalProductionType.SERIES).build()));

        assertThatThrownBy(() -> service.create(create("baska", List.of("got"))))
                .isInstanceOf(DuplicateResourceException.class);

        verify(portalRepository, never()).save(any());
    }

    @Test
    void create_imageUrlFromForeignStorage_is400() {
        when(portalRepository.existsBySlug(anyString())).thenReturn(false);
        when(mediaUrlValidator.isOwnedImage("https://evil.example/x.png")).thenReturn(false);
        PortalCreateRequest request = new PortalCreateRequest("yeni", "Ad", "Name", null, null,
                "https://evil.example/x.png", null, null, null, null, null, null);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void create_ownedImageUrl_isStored() {
        when(portalRepository.existsBySlug(anyString())).thenReturn(false);
        when(mediaUrlValidator.isOwnedImage(IMG)).thenReturn(true);

        AdminPortalResponse response = service.create(new PortalCreateRequest("yeni", "Ad", "Name", null, null,
                IMG, IMG, PortalPostingPolicy.STAFF_ONLY, PortalStatus.HIDDEN, null, 7, null));

        assertThat(response.bannerUrl()).isEqualTo(IMG);
        assertThat(response.postingPolicy()).isEqualTo(PortalPostingPolicy.STAFF_ONLY);
        assertThat(response.status()).isEqualTo(PortalStatus.HIDDEN);
        assertThat(response.sortOrder()).isEqualTo(7);
    }

    // ---- update ----

    @Test
    void update_nullFieldsAreUnchanged_setFieldsApplied() {
        Portal p = existing();
        p.setDescriptionTr("eski");
        when(portalRepository.findBySlug("westeros")).thenReturn(Optional.of(p));

        AdminPortalResponse response = service.update("westeros", new PortalUpdateRequest(null, "Yeni Ad", null, null,
                null, null, null, PortalPostingPolicy.STAFF_ONLY, null, null, 3, null));

        assertThat(response.nameTr()).isEqualTo("Yeni Ad");
        assertThat(response.nameEn()).isEqualTo("Westeros");
        assertThat(response.descriptionTr()).isEqualTo("eski");
        assertThat(response.postingPolicy()).isEqualTo(PortalPostingPolicy.STAFF_ONLY);
        assertThat(response.sortOrder()).isEqualTo(3);
        assertThat(response.status()).isEqualTo(PortalStatus.ACTIVE);
    }

    @Test
    void update_slugChange_is400_sameSlugAccepted() {
        when(portalRepository.findBySlug("westeros")).thenReturn(Optional.of(existing()));

        assertThatThrownBy(() -> service.update("westeros", new PortalUpdateRequest("baska", null, null, null, null,
                null, null, null, null, null, null, null))).isInstanceOf(InvalidReferenceException.class);

        assertThat(service.update("westeros", new PortalUpdateRequest("westeros", null, null, null, null,
                null, null, null, null, null, null, null)).slug()).isEqualTo("westeros");
    }

    @Test
    void update_unknownPortal_is404() {
        when(portalRepository.findBySlug("yok")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("yok", emptyUpdate())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_blankImageUrl_clearsImage() {
        Portal p = existing();
        p.setBannerUrl(IMG);
        when(portalRepository.findBySlug("westeros")).thenReturn(Optional.of(p));

        AdminPortalResponse response = service.update("westeros", new PortalUpdateRequest(null, null, null, null,
                null, "", null, null, null, null, null, null));

        assertThat(response.bannerUrl()).isNull();
    }

    @Test
    void update_productionSlugs_diffsRemovedAndAddedOnly() {
        when(portalRepository.findBySlug("westeros")).thenReturn(Optional.of(existing()));
        when(portalProductionRepository.findByPortalId(1L)).thenReturn(List.of(
                PortalProduction.builder().portalId(1L).productionSlug("got").productionType(PortalProductionType.SERIES).build(),
                PortalProduction.builder().portalId(1L).productionSlug("eski").productionType(PortalProductionType.SERIES).build()));
        when(movieService.existsBySlug("hotd")).thenReturn(false);
        when(seriesService.existsBySlug("hotd")).thenReturn(true);
        when(portalProductionRepository.findByProductionSlug("hotd")).thenReturn(Optional.empty());

        service.update("westeros", new PortalUpdateRequest(null, null, null, null, null, null, null, null, null,
                List.of("got", "hotd"), null, null));

        verify(portalProductionRepository).deleteByPortalIdAndProductionSlugIn(eq(1L), eq(List.of("eski")));
        ArgumentCaptor<List<PortalProduction>> added = ArgumentCaptor.forClass(List.class);
        verify(portalProductionRepository).saveAll(added.capture());
        assertThat(added.getValue()).extracting(PortalProduction::getProductionSlug).containsExactly("hotd");
    }

    @Test
    void update_emptyProductionList_removesAll_nullLeavesUntouched() {
        when(portalRepository.findBySlug("westeros")).thenReturn(Optional.of(existing()));
        when(portalProductionRepository.findByPortalId(1L)).thenReturn(List.of(
                PortalProduction.builder().portalId(1L).productionSlug("got").productionType(PortalProductionType.SERIES).build()));

        service.update("westeros", new PortalUpdateRequest(null, null, null, null, null, null, null, null, null,
                List.of(), null, null));
        verify(portalProductionRepository).deleteByPortalIdAndProductionSlugIn(eq(1L), eq(List.of("got")));

        org.mockito.Mockito.clearInvocations(portalProductionRepository);
        service.update("westeros", emptyUpdate());
        verify(portalProductionRepository, never()).deleteByPortalIdAndProductionSlugIn(any(), anyCollection());
        verify(portalProductionRepository, never()).findByPortalId(any());
    }

    // ---- archive / unarchive ----

    @Test
    void archive_setsArchived_unarchive_setsActive() {
        Portal p = existing();
        when(portalRepository.findBySlug("westeros")).thenReturn(Optional.of(p));

        assertThat(service.archive("westeros").status()).isEqualTo(PortalStatus.ARCHIVED);
        assertThat(service.unarchive("westeros").status()).isEqualTo(PortalStatus.ACTIVE);
    }

    @Test
    void archive_isIdempotent() {
        Portal p = existing();
        p.setStatus(PortalStatus.ARCHIVED);
        when(portalRepository.findBySlug("westeros")).thenReturn(Optional.of(p));

        assertThat(service.archive("westeros").status()).isEqualTo(PortalStatus.ARCHIVED);
    }

    @Test
    void archiveAndUnarchive_hiddenPortal_is400_soHiddenNeverBecomesVisibleBySideEffect() {
        Portal p = existing();
        p.setStatus(PortalStatus.HIDDEN);
        when(portalRepository.findBySlug("westeros")).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.archive("westeros")).isInstanceOf(InvalidReferenceException.class);
        assertThatThrownBy(() -> service.unarchive("westeros")).isInstanceOf(InvalidReferenceException.class);
        assertThat(p.getStatus()).isEqualTo(PortalStatus.HIDDEN);
    }

    // ---- list ----

    @Test
    void list_includesHiddenPortals_andResolvesProductionSlugsWithOneQuery() {
        Portal a = existing();
        Portal hidden = Portal.builder().id(2L).slug("gizli").nameTr("G").nameEn("G").status(PortalStatus.HIDDEN).build();
        Pageable pageable = PageRequest.of(0, 20);
        when(portalRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(a, hidden), pageable, 2));
        when(portalProductionRepository.findByPortalIdIn(List.of(1L, 2L))).thenReturn(List.of(
                PortalProduction.builder().portalId(1L).productionSlug("got").productionType(PortalProductionType.SERIES).build()));

        PageResponse<AdminPortalResponse> page = service.list(pageable);

        assertThat(page.content()).extracting(AdminPortalResponse::slug).containsExactly("westeros", "gizli");
        assertThat(page.content().get(0).productionSlugs()).containsExactly("got");
        assertThat(page.content().get(1).productionSlugs()).isEmpty();
        verify(portalProductionRepository).findByPortalIdIn(any());
    }

    // ---- accentColor (#RRGGBB, opsiyonel; backend yalnız saklar) ----

    @Test
    void create_accentColor_isStored_andBlankMeansNone() {
        when(portalRepository.existsBySlug(anyString())).thenReturn(false);

        AdminPortalResponse with = service.create(new PortalCreateRequest("yeni", "Ad", "Name", null, null,
                null, null, null, null, null, null, "#C0392B"));
        AdminPortalResponse blank = service.create(new PortalCreateRequest("yeni2", "Ad", "Name", null, null,
                null, null, null, null, null, null, ""));

        assertThat(with.accentColor()).isEqualTo("#C0392B");
        assertThat(blank.accentColor()).isNull();
    }

    @Test
    void update_accentColor_nullUnchanged_valueSets_emptyClears() {
        Portal p = existing();
        p.setAccentColor("#111111");
        when(portalRepository.findBySlug("westeros")).thenReturn(Optional.of(p));

        assertThat(service.update("westeros", new PortalUpdateRequest(null, null, null, null, null, null, null,
                null, null, null, null, null)).accentColor()).isEqualTo("#111111");
        assertThat(service.update("westeros", new PortalUpdateRequest(null, null, null, null, null, null, null,
                null, null, null, null, "#AABBCC")).accentColor()).isEqualTo("#AABBCC");
        assertThat(service.update("westeros", new PortalUpdateRequest(null, null, null, null, null, null, null,
                null, null, null, null, "")).accentColor()).isNull();
    }

    @Test
    void accentColor_beanValidation_onlyAcceptsSixDigitHexOrEmpty() {
        jakarta.validation.Validator validator =
                jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
        java.util.function.Function<String, Boolean> valid = color -> validator.validate(new PortalCreateRequest(
                "yeni", "Ad", "Name", null, null, null, null, null, null, null, null, color)).isEmpty();

        assertThat(valid.apply(null)).isTrue();
        assertThat(valid.apply("")).isTrue();
        assertThat(valid.apply("#c0392b")).isTrue();
        assertThat(valid.apply("#C0392B")).isTrue();
        assertThat(valid.apply("C0392B")).isFalse();      // # yok
        assertThat(valid.apply("#FFF")).isFalse();         // kısa hex kabul edilmez
        assertThat(valid.apply("#GGGGGG")).isFalse();
        assertThat(valid.apply("red")).isFalse();
        assertThat(valid.apply("#C0392B;background:url(x)")).isFalse();
    }
}
