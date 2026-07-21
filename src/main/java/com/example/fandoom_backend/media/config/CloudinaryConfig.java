package com.example.fandoom_backend.media.config;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        // CLOUDINARY_URL ortam değişkeninden (cloudinary://api_key:api_secret@cloud_name) otomatik okunur
        return new Cloudinary();
    }
}
