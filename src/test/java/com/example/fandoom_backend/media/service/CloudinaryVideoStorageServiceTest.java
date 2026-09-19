package com.example.fandoom_backend.media.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.example.fandoom_backend.common.exception.InvalidFileException;
import com.example.fandoom_backend.media.dto.MediaUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryVideoStorageServiceTest {

    @Mock private Cloudinary cloudinary;
    @Mock private Uploader uploader;

    private CloudinaryVideoStorageService service;

    @BeforeEach
    void setUp() {
        service = new CloudinaryVideoStorageService(cloudinary);
    }

    @Test
    void upload_acceptedTypes_uploadWithVideoResourceTypeAndFolder() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenReturn(
                Map.of("secure_url", "https://res.cloudinary.com/demo/video/upload/v1/fandoom/community/a.mp4",
                        "public_id", "fandoom/community/a"));

        for (String type : new String[]{"video/mp4", "video/webm", "video/quicktime"}) {
            MediaUploadResponse response = service.upload(
                    new MockMultipartFile("file", "a", type, new byte[]{1, 2, 3}), "community");
            assertThat(response.url()).endsWith("a.mp4");
            assertThat(response.publicId()).isEqualTo("fandoom/community/a");
        }

        ArgumentCaptor<Map<String, Object>> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader, org.mockito.Mockito.times(3)).upload(any(byte[].class), options.capture());
        assertThat(options.getValue()).containsEntry("resource_type", "video")
                .containsEntry("folder", "fandoom/community");
    }

    @Test
    void upload_unsupportedType_throwsInvalidFile() {
        assertThatThrownBy(() -> service.upload(
                new MockMultipartFile("file", "a.avi", "video/x-msvideo", new byte[]{1}), "community"))
                .isInstanceOf(InvalidFileException.class);
        assertThatThrownBy(() -> service.upload(
                new MockMultipartFile("file", "a.png", "image/png", new byte[]{1}), "community"))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void upload_emptyFile_throwsInvalidFile() {
        assertThatThrownBy(() -> service.upload(
                new MockMultipartFile("file", "a.mp4", "video/mp4", new byte[0]), "community"))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void upload_over50MB_throwsInvalidFile_andNeverCallsCloudinary() {
        MultipartFile big = mock(MultipartFile.class);
        when(big.isEmpty()).thenReturn(false);
        when(big.getContentType()).thenReturn("video/mp4");
        when(big.getSize()).thenReturn(51L * 1024 * 1024);

        assertThatThrownBy(() -> service.upload(big, "community")).isInstanceOf(InvalidFileException.class);

        verify(cloudinary, never()).uploader();
    }

    @Test
    void upload_exactly50MB_isAccepted() throws Exception {
        MultipartFile limit = mock(MultipartFile.class);
        when(limit.isEmpty()).thenReturn(false);
        when(limit.getContentType()).thenReturn("video/mp4");
        when(limit.getSize()).thenReturn(50L * 1024 * 1024);
        when(limit.getBytes()).thenReturn(new byte[]{1});
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenReturn(Map.of("secure_url", "u", "public_id", "p"));

        assertThat(service.upload(limit, "community").url()).isEqualTo("u");
    }

    @Test
    void delete_destroysWithVideoResourceType() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);

        service.delete("https://res.cloudinary.com/demo/video/upload/v1/fandoom/community/a.mp4");

        ArgumentCaptor<Map<String, Object>> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).destroy(eq("fandoom/community/a"), options.capture());
        assertThat(options.getValue()).containsEntry("resource_type", "video");
    }

    @Test
    void delete_unparsableUrl_isNoOp() {
        service.delete("bu-bir-url-degil");
        service.delete(null);

        verify(cloudinary, never()).uploader();
    }
}
