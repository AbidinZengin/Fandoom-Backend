package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.community.dto.PortalSearchResult;
import com.example.fandoom_backend.community.entity.PortalStatus;
import com.example.fandoom_backend.community.repository.PortalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalQueryServiceImplQuickSearchTest {

    @Mock
    private PortalRepository portalRepository;

    @InjectMocks
    private PortalQueryServiceImpl portalQueryService;

    @Test
    void quickSearch_onlyPublicStatuses_neverHidden() {
        PortalSearchResult dune = new PortalSearchResult(2L, "dune", "Dune", "Dune", null);
        when(portalRepository.quickSearch(EnumSet.of(PortalStatus.ACTIVE, PortalStatus.ARCHIVED),
                "%dune%", "dune%", PageRequest.of(0, 5))).thenReturn(List.of(dune));

        List<PortalSearchResult> result = portalQueryService.quickSearch("dune", 5);

        assertThat(result).containsExactly(dune);
    }
}
