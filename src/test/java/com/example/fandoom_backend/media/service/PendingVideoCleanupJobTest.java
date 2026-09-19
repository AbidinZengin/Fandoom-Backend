package com.example.fandoom_backend.media.service;

import com.cloudinary.Api;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.api.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingVideoCleanupJobTest {

    private static final Instant NOW = Instant.parse("2026-09-19T12:00:00Z");

    @Mock private Cloudinary cloudinary;
    @Mock private Api api;
    @Mock private Uploader uploader;
    @Mock private MediaReferenceChecker referenceChecker;

    private PendingVideoCleanupJob job;

    @BeforeEach
    void setUp() {
        job = new PendingVideoCleanupJob(cloudinary, List.of(referenceChecker), 24);
        lenient().when(cloudinary.api()).thenReturn(api);
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
        lenient().when(referenceChecker.isReferenced(any())).thenReturn(false);
    }

    private static Map<String, Object> resource(String publicId, Instant createdAt) {
        Map<String, Object> resource = new HashMap<>();
        resource.put("public_id", publicId);
        resource.put("secure_url", "https://res.cloudinary.com/demo/video/upload/v1/" + publicId + ".mp4");
        if (createdAt != null) {
            resource.put("created_at", createdAt.toString());
        }
        return resource;
    }

    // Düz Map (mock değil): thenReturn(page(...)) içinde iç içe mock/stub Mockito'da UnfinishedStubbing verir.
    // ApiResponse ham Map'i genişlettiği için ham tip gerekir.
    @SuppressWarnings({"rawtypes", "unchecked"})
    static class FakeApiResponse extends HashMap implements ApiResponse {
        @Override public Map<String, com.cloudinary.api.RateLimit> rateLimits() { return Map.of(); }
        @Override public com.cloudinary.api.RateLimit apiRateLimit() { return null; }
    }

    private ApiResponse page(List<Map<String, Object>> resources, String nextCursor) {
        FakeApiResponse response = new FakeApiResponse();
        response.put("resources", resources);
        if (nextCursor != null) {
            response.put("next_cursor", nextCursor);
        }
        return response;
    }

    private void listing(List<Map<String, Object>> resources) throws Exception {
        when(api.resourcesByTag(eq("pending"), anyMap())).thenReturn(page(resources, null));
    }

    @SuppressWarnings("unchecked")
    private List<String> deletedIds() throws Exception {
        ArgumentCaptor<Iterable<String>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(api, org.mockito.Mockito.atLeastOnce()).deleteResources(captor.capture(), anyMap());
        List<String> ids = new ArrayList<>();
        captor.getAllValues().forEach(batch -> batch.forEach(ids::add));
        return ids;
    }

    // ---- yaş korumaları: kullanıcı hâlâ gönderisini yazıyor olabilir ----

    @Test
    void assetsYoungerThan24Hours_areNeverDeleted() throws Exception {
        listing(List.of(
                resource("fandoom/community/just-now", NOW),
                resource("fandoom/community/10-min", NOW.minusSeconds(600)),
                resource("fandoom/community/59-min", NOW.minusSeconds(59 * 60)),
                resource("fandoom/community/1-hour", NOW.minusSeconds(3600)),
                resource("fandoom/community/6-hours", NOW.minusSeconds(6 * 3600)),
                resource("fandoom/community/23h59m", NOW.minusSeconds(24 * 3600 - 60)),
                resource("fandoom/community/exactly-24h", NOW.minusSeconds(24 * 3600))));

        int deleted = job.cleanup(NOW);

        assertThat(deleted).isZero();
        verify(api, never()).deleteResources(any(), anyMap());
    }

    @Test
    void assetsOlderThan24Hours_areDeleted_asVideoResourceType() throws Exception {
        listing(List.of(
                resource("fandoom/community/old-1", NOW.minusSeconds(24 * 3600 + 1)),
                resource("fandoom/community/old-2", NOW.minusSeconds(72 * 3600)),
                resource("fandoom/community/fresh", NOW.minusSeconds(3600))));

        int deleted = job.cleanup(NOW);

        assertThat(deleted).isEqualTo(2);
        assertThat(deletedIds()).containsExactlyInAnyOrder("fandoom/community/old-1", "fandoom/community/old-2");
        ArgumentCaptor<Map<String, Object>> options = ArgumentCaptor.forClass(Map.class);
        verify(api).deleteResources(any(), options.capture());
        assertThat(options.getValue()).containsEntry("resource_type", "video");
    }

    @Test
    void assetWithMissingOrUnparsableCreatedAt_isNeverDeleted() throws Exception {
        Map<String, Object> garbage = resource("fandoom/community/garbage", null);
        garbage.put("created_at", "dün gibi");
        listing(List.of(resource("fandoom/community/no-date", null), garbage));

        assertThat(job.cleanup(NOW)).isZero();
        verify(api, never()).deleteResources(any(), anyMap());
    }

    // ---- kullanımdaki video ----

    @Test
    void oldAssetStillReferencedByContent_isNotDeleted_butItsPendingTagIsFixed() throws Exception {
        Map<String, Object> inUse = resource("fandoom/community/in-use", NOW.minusSeconds(48 * 3600));
        when(referenceChecker.isReferenced((String) inUse.get("secure_url"))).thenReturn(true);
        listing(List.of(inUse, resource("fandoom/community/orphan", NOW.minusSeconds(48 * 3600))));

        int deleted = job.cleanup(NOW);

        assertThat(deleted).isEqualTo(1);
        assertThat(deletedIds()).containsExactly("fandoom/community/orphan");
        verify(uploader).removeTag(eq("pending"), eq(new String[]{"fandoom/community/in-use"}), anyMap());
    }

    @Test
    void referenceCheckIsNotEvenAskedForYoungAssets() throws Exception {
        listing(List.of(resource("fandoom/community/young", NOW.minusSeconds(3600))));

        job.cleanup(NOW);

        verify(referenceChecker, never()).isReferenced(any());
    }

    // ---- sayfalama / toplu silme ----

    @Test
    void followsPaginationCursor_andDeletesInBatchesOf100() throws Exception {
        List<Map<String, Object>> first = IntStream.range(0, 150)
                .mapToObj(i -> resource("fandoom/community/a" + i, NOW.minusSeconds(48 * 3600))).toList();
        List<Map<String, Object>> second = IntStream.range(0, 30)
                .mapToObj(i -> resource("fandoom/community/b" + i, NOW.minusSeconds(48 * 3600))).toList();
        when(api.resourcesByTag(eq("pending"), anyMap()))
                .thenReturn(page(first, "cursor-2"))
                .thenReturn(page(second, null));

        int deleted = job.cleanup(NOW);

        assertThat(deleted).isEqualTo(180);
        verify(api, times(2)).resourcesByTag(eq("pending"), anyMap());
        verify(api, times(2)).deleteResources(any(), anyMap()); // 100 + 80
    }

    @Test
    void failedDeleteBatch_doesNotAbortTheRun() throws Exception {
        listing(List.of(resource("fandoom/community/old", NOW.minusSeconds(48 * 3600))));
        when(api.deleteResources(any(), anyMap())).thenThrow(new RuntimeException("cloudinary down"));

        assertThat(job.cleanup(NOW)).isZero();
    }

    @Test
    void run_swallowsListingFailure() throws Exception {
        when(api.resourcesByTag(eq("pending"), anyMap())).thenThrow(new RuntimeException("rate limited"));

        job.run(); // fırlatmamalı
    }

    // ---- yapılandırma tabanı ----

    @Test
    void maxAgeBelowOneHour_isRejectedAtStartup() {
        assertThatThrownBy(() -> new PendingVideoCleanupJob(cloudinary, List.of(), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PendingVideoCleanupJob(cloudinary, List.of(), -5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void maxAgeOfExactlyOneHour_isAccepted_andHonoured() throws Exception {
        PendingVideoCleanupJob strict = new PendingVideoCleanupJob(cloudinary, List.of(referenceChecker), 1);
        listing(List.of(
                resource("fandoom/community/59-min", NOW.minusSeconds(59 * 60)),
                resource("fandoom/community/2-hours", NOW.minusSeconds(2 * 3600))));

        assertThat(strict.cleanup(NOW)).isEqualTo(1);
        assertThat(deletedIds()).containsExactly("fandoom/community/2-hours");
    }
}
