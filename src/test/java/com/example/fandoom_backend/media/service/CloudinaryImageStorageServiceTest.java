package com.example.fandoom_backend.media.service;

import com.cloudinary.Cloudinary;
import com.example.fandoom_backend.common.exception.InvalidFileException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryImageStorageServiceTest {

    @Mock private Cloudinary cloudinary;
    @Mock private ImageCompressor imageCompressor;

    @Test
    void upload_over10MB_throwsInvalidFile_beforeCompressionOrUpload() {
        MultipartFile big = mock(MultipartFile.class);
        when(big.isEmpty()).thenReturn(false);
        when(big.getContentType()).thenReturn("image/png");
        when(big.getSize()).thenReturn(10L * 1024 * 1024 + 1);

        assertThatThrownBy(() -> new CloudinaryImageStorageService(cloudinary, imageCompressor).upload(big, "community"))
                .isInstanceOf(InvalidFileException.class);

        verify(cloudinary, never()).uploader();
    }
}
