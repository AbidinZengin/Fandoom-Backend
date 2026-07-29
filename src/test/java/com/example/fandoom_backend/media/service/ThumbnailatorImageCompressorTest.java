package com.example.fandoom_backend.media.service;

import com.example.fandoom_backend.common.exception.InvalidFileException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThumbnailatorImageCompressorTest {

    private final ImageCompressor compressor = new ThumbnailatorImageCompressor(2000, 2000, 0.82f);

    @Test
    void compress_pngToWebp_producesValidWebpBytes() throws IOException {
        byte[] png = renderPng(400, 300);

        byte[] result = compressor.compress(png);

        assertThat(result).isNotEmpty();
        // WebP magic bytes: "RIFF"....."WEBP"
        assertThat(new String(result, 0, 4)).isEqualTo("RIFF");
        assertThat(new String(result, 8, 4)).isEqualTo("WEBP");
    }

    @Test
    void compress_largerThanMaxWidth_isDownscaled() throws IOException {
        ImageCompressor smallBoxCompressor = new ThumbnailatorImageCompressor(100, 100, 0.82f);
        byte[] png = renderPng(400, 300);

        byte[] result = smallBoxCompressor.compress(png);

        BufferedImage decoded = ImageIO.read(new java.io.ByteArrayInputStream(result));
        assertThat(decoded.getWidth()).isLessThanOrEqualTo(100);
        assertThat(decoded.getHeight()).isLessThanOrEqualTo(100);
    }

    @Test
    void compress_smallerThanMaxWidth_isNotUpscaled() throws IOException {
        byte[] png = renderPng(50, 40);

        byte[] result = compressor.compress(png);

        BufferedImage decoded = ImageIO.read(new java.io.ByteArrayInputStream(result));
        assertThat(decoded.getWidth()).isEqualTo(50);
        assertThat(decoded.getHeight()).isEqualTo(40);
    }

    @Test
    void compress_corruptBytes_throwsInvalidFileException() {
        byte[] garbage = "not an image".getBytes();

        assertThatThrownBy(() -> compressor.compress(garbage))
                .isInstanceOf(InvalidFileException.class);
    }

    private byte[] renderPng(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", out)) {
            throw new UncheckedIOException(new IOException("PNG writer bulunamadı"));
        }
        return out.toByteArray();
    }
}
