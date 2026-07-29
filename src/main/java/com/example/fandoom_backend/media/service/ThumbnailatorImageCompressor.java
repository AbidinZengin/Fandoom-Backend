package com.example.fandoom_backend.media.service;

import com.example.fandoom_backend.common.exception.InvalidFileException;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class ThumbnailatorImageCompressor implements ImageCompressor {

    private final int maxWidth;
    private final int maxHeight;
    private final float quality;

    public ThumbnailatorImageCompressor(
            @Value("${media.image.max-width:2000}") int maxWidth,
            @Value("${media.image.max-height:2000}") int maxHeight,
            @Value("${media.image.quality:0.82}") float quality) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.quality = quality;
    }

    @Override
    public byte[] compress(byte[] original) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
            if (source == null) {
                throw new InvalidFileException("Görsel işlenemedi, dosya bozuk olabilir");
            }
            // hedef kutu, orijinali asla buyutmesin diye kendi boyutuyla sinirlanir
            int targetWidth = Math.min(source.getWidth(), maxWidth);
            int targetHeight = Math.min(source.getHeight(), maxHeight);

            Thumbnails.of(source)
                    .size(targetWidth, targetHeight)
                    .outputFormat("webp")
                    .outputQuality(quality)
                    .toOutputStream(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new InvalidFileException("Görsel işlenemedi, dosya bozuk olabilir");
        }
    }
}
