package com.example.fandoom_backend.media.service;

import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryMediaUrlValidatorTest {

    private final CloudinaryMediaUrlValidator validator =
            new CloudinaryMediaUrlValidator(new Cloudinary("cloudinary://key:secret@demo"));

    @Test
    void ownedImageAndVideo_matchOnlyTheirOwnResourceType() {
        String image = "https://res.cloudinary.com/demo/image/upload/v123/fandoom/community/a.webp";
        String video = "https://res.cloudinary.com/demo/video/upload/v123/fandoom/community/a.mp4";

        assertThat(validator.isOwnedImage(image)).isTrue();
        assertThat(validator.isOwnedVideo(image)).isFalse();
        assertThat(validator.isOwnedVideo(video)).isTrue();
        assertThat(validator.isOwnedImage(video)).isFalse();
    }

    @Test
    void rejectsOtherCloudHostSchemeAndTrickyUrls() {
        assertThat(validator.isOwnedImage("https://res.cloudinary.com/baska/image/upload/v1/a.webp")).isFalse();
        assertThat(validator.isOwnedImage("http://res.cloudinary.com/demo/image/upload/v1/a.webp")).isFalse();
        assertThat(validator.isOwnedImage("https://evil.com/demo/image/upload/v1/a.webp")).isFalse();
        assertThat(validator.isOwnedImage("https://res.cloudinary.com@evil.com/demo/image/upload/v1/a.webp")).isFalse();
        assertThat(validator.isOwnedImage("https://res.cloudinary.com/demo/image/upload/v1/a.webp?x=1")).isFalse();
        assertThat(validator.isOwnedImage("https://res.cloudinary.com/demo/image/upload/../../x.webp")).isFalse();
        assertThat(validator.isOwnedImage("https://res.cloudinary.com/demo/image/upload/")).isFalse();
        assertThat(validator.isOwnedImage("https://res.cloudinary.com/demo/image/fetch/https://x.com/a.png")).isFalse();
        assertThat(validator.isOwnedImage("not a url")).isFalse();
        assertThat(validator.isOwnedImage(null)).isFalse();
        assertThat(validator.isOwnedImage("")).isFalse();
    }

    @Test
    void isInFolder_matchesOnlyFandoomFolderPrefix() {
        assertThat(validator.isInFolder(
                "https://res.cloudinary.com/demo/image/upload/v1/fandoom/community/a.webp", "community")).isTrue();
        assertThat(validator.isInFolder(
                "https://res.cloudinary.com/demo/video/upload/fandoom/community/a.mp4", "community")).isTrue();
        assertThat(validator.isInFolder(
                "https://res.cloudinary.com/demo/image/upload/v1/fandoom/movies/a.webp", "community")).isFalse();
        assertThat(validator.isInFolder(
                "https://res.cloudinary.com/demo/image/upload/v1/fandoom/communityx/a.webp", "community")).isFalse();
        assertThat(validator.isInFolder(
                "https://res.cloudinary.com/demo/image/upload/v1/other/fandoom/community/a.webp", "community")).isFalse();
    }

    @Test
    void withoutCloudName_nothingIsValid() {
        // CLOUDINARY_URL set edilmemiş ortam: fail closed
        CloudinaryMediaUrlValidator unconfigured = new CloudinaryMediaUrlValidator(
                new Cloudinary(new java.util.HashMap<String, Object>()));

        assertThat(unconfigured.isOwnedImage("https://res.cloudinary.com/demo/image/upload/v1/a.webp")).isFalse();
    }
}
