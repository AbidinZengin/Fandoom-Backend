package com.example.fandoom_backend.media.service;

import com.cloudinary.Cloudinary;
import com.example.fandoom_backend.media.dto.VideoUploadSignatureResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CloudinaryVideoUploadSignerTest {

    private final CloudinaryVideoUploadSigner signer = new CloudinaryVideoUploadSigner(
            new Cloudinary("cloudinary://key123:secret456@demo"), "community", 120);

    @Test
    void createSignature_returnsPendingTaggedCommunityUploadParams() {
        VideoUploadSignatureResponse response = signer.createSignature();

        assertThat(response.uploadUrl()).isEqualTo("https://api.cloudinary.com/v1_1/demo/video/upload");
        assertThat(response.apiKey()).isEqualTo("key123");
        assertThat(response.params()).containsEntry("folder", "fandoom/community")
                .containsEntry("tags", "pending")
                .containsEntry("allowed_formats", "mp4,webm,mov")
                .containsEntry("eager_async", "true")
                .containsKey("timestamp");
        assertThat(response.maxFileSizeBytes()).isEqualTo(50L * 1024 * 1024);
        assertThat(response.maxDurationSeconds()).isEqualTo(120);
    }

    // Bağımsız doğrulama: Cloudinary imza algoritması = SHA-1(alfabetik "k=v&k=v" + api_secret).
    // api_key/file/resource_type imzaya girmez; params'taki HER şey girer (değiştirilirse Cloudinary reddeder).
    @Test
    void createSignature_signsExactlyTheReturnedParams() throws Exception {
        VideoUploadSignatureResponse response = signer.createSignature();

        String toSign = new TreeMap<>(response.params()).entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        byte[] digest = MessageDigest.getInstance("SHA-1").digest((toSign + "secret456").getBytes(StandardCharsets.UTF_8));

        assertThat(response.signature()).isEqualTo(HexFormat.of().formatHex(digest));
    }

    @Test
    void createSignature_neverLeaksApiSecret() {
        VideoUploadSignatureResponse response = signer.createSignature();

        assertThat(response.toString()).doesNotContain("secret456");
    }

    @Test
    void createSignature_withoutCloudinaryConfig_failsClosed() {
        CloudinaryVideoUploadSigner unconfigured = new CloudinaryVideoUploadSigner(
                new Cloudinary(new java.util.HashMap<String, Object>()), "community", 120);

        assertThatThrownBy(unconfigured::createSignature).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createSignature_timestampIsFresh() {
        Map<String, String> params = signer.createSignature().params();

        long now = System.currentTimeMillis() / 1000;
        assertThat(Long.parseLong(params.get("timestamp"))).isBetween(now - 5, now + 5);
    }
}
