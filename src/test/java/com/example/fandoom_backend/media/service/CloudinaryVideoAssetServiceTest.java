package com.example.fandoom_backend.media.service;

import com.cloudinary.Api;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.api.ApiResponse;
import com.cloudinary.api.exceptions.NotFound;
import com.cloudinary.api.exceptions.RateLimited;
import com.example.fandoom_backend.common.exception.InvalidFileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryVideoAssetServiceTest {

    private static final String URL = "https://res.cloudinary.com/demo/video/upload/v1/fandoom/community/a.mp4";
    private static final String PUBLIC_ID = "fandoom/community/a";

    @Mock private Cloudinary cloudinary;
    @Mock private Api api;
    @Mock private Uploader uploader;

    private CloudinaryVideoAssetService service;

    @BeforeEach
    void setUp() {
        service = new CloudinaryVideoAssetService(cloudinary, 120);
        org.mockito.Mockito.lenient().when(cloudinary.api()).thenReturn(api);
        org.mockito.Mockito.lenient().when(cloudinary.uploader()).thenReturn(uploader);
    }

    // ApiResponse bir Map: gerçek Cloudinary cevabının ilgili alanlarını taklit eden düz bir Map (mock değil:
    // when(...).thenReturn(...) içinde iç içe mock oluşturmak Mockito'da UnfinishedStubbing verir).
    // ApiResponse ham Map'i genişlettiği için ham tip gerekir.
    @SuppressWarnings({"rawtypes", "unchecked"})
    static class FakeApiResponse extends HashMap implements ApiResponse {
        @Override public Map<String, com.cloudinary.api.RateLimit> rateLimits() { return Map.of(); }
        @Override public com.cloudinary.api.RateLimit apiRateLimit() { return null; }
    }

    private ApiResponse asset(long bytes, double duration, String format, List<String> tags) {
        FakeApiResponse response = new FakeApiResponse();
        response.put("bytes", bytes);
        response.put("duration", duration);
        response.put("format", format);
        response.put("tags", tags);
        return response;
    }

    @Test
    void verify_withinLimits_passes_andReadsRealAssetFromVideoResourceType() throws Exception {
        when(api.resource(eq(PUBLIC_ID), anyMap())).thenReturn(asset(10_000_000, 30.5, "mp4", List.of("pending")));

        service.verifyWithinLimits(URL);

        ArgumentCaptor<Map<String, Object>> options = ArgumentCaptor.forClass(Map.class);
        verify(api).resource(eq(PUBLIC_ID), options.capture());
        assertThat(options.getValue()).containsEntry("resource_type", "video");
        verify(uploader, never()).destroy(any(), anyMap());
    }

    @Test
    void verify_exactlyAtLimits_passes() throws Exception {
        when(api.resource(eq(PUBLIC_ID), anyMap())).thenReturn(asset(50L * 1024 * 1024, 120.0, "webm", List.of()));

        service.verifyWithinLimits(URL);
    }

    @Test
    void verify_over50MB_isRejected_andPendingAssetIsDestroyed() throws Exception {
        when(api.resource(eq(PUBLIC_ID), anyMap())).thenReturn(asset(50L * 1024 * 1024 + 1, 30, "mp4", List.of("pending")));

        assertThatThrownBy(() -> service.verifyWithinLimits(URL)).isInstanceOf(InvalidFileException.class);

        ArgumentCaptor<Map<String, Object>> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).destroy(eq(PUBLIC_ID), options.capture());
        assertThat(options.getValue()).containsEntry("resource_type", "video");
    }

    @Test
    void verify_overDuration_isRejected() throws Exception {
        when(api.resource(eq(PUBLIC_ID), anyMap())).thenReturn(asset(1_000, 120.5, "mp4", List.of("pending")));

        assertThatThrownBy(() -> service.verifyWithinLimits(URL))
                .isInstanceOf(InvalidFileException.class).hasMessageContaining("120");
    }

    @Test
    void verify_unsupportedFormat_isRejected() throws Exception {
        when(api.resource(eq(PUBLIC_ID), anyMap())).thenReturn(asset(1_000, 10, "avi", List.of("pending")));

        assertThatThrownBy(() -> service.verifyWithinLimits(URL)).isInstanceOf(InvalidFileException.class);
    }

    // Başka bir içeriğe zaten bağlı (pending kalkmış) video, limit dışı görünse bile bu yoldan SİLİNMEZ.
    @Test
    void verify_overLimitButNotPending_isRejectedWithoutDestroying() throws Exception {
        when(api.resource(eq(PUBLIC_ID), anyMap())).thenReturn(asset(60L * 1024 * 1024, 30, "mp4", List.of()));

        assertThatThrownBy(() -> service.verifyWithinLimits(URL)).isInstanceOf(InvalidFileException.class);

        verify(uploader, never()).destroy(any(), anyMap());
    }

    @Test
    void verify_missingSizeOrDuration_failsClosed() throws Exception {
        when(api.resource(eq(PUBLIC_ID), anyMap())).thenReturn(new FakeApiResponse());

        assertThatThrownBy(() -> service.verifyWithinLimits(URL)).isInstanceOf(InvalidFileException.class);
    }

    @Test
    void verify_assetNotFound_isInvalidFile() throws Exception {
        when(api.resource(eq(PUBLIC_ID), anyMap())).thenThrow(new NotFound("Resource not found"));

        assertThatThrownBy(() -> service.verifyWithinLimits(URL)).isInstanceOf(InvalidFileException.class);
    }

    @Test
    void verify_storageUnavailableOrRateLimited_failsClosedInsteadOfSkippingLimits() throws Exception {
        when(api.resource(eq(PUBLIC_ID), anyMap())).thenThrow(new RateLimited("Rate Limit Exceeded"));

        assertThatThrownBy(() -> service.verifyWithinLimits(URL)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void verify_unparsableUrl_isInvalidFile() {
        assertThatThrownBy(() -> service.verifyWithinLimits("bu-bir-url-degil")).isInstanceOf(InvalidFileException.class);

        verify(cloudinary, never()).api();
    }

    @Test
    void markAttached_removesPendingTagFromVideoAsset() throws Exception {
        service.markAttached(URL);

        ArgumentCaptor<Map<String, Object>> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).removeTag(eq("pending"), eq(new String[]{PUBLIC_ID}), options.capture());
        assertThat(options.getValue()).containsEntry("resource_type", "video");
    }
}
