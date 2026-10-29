package com.example.fandoom_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class FandoomBackendApplication {

	public static void main(String[] args) {
		// Tüm LocalDateTime'lar (auditing, token süreleri, tekrar engelleri) sunucunun yerel saatine değil UTC'ye göre
		// üretilsin: sunucu nerede çalışırsa çalışsın aynı sonuç. Context'ten ÖNCE ayarlanmalı.
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(FandoomBackendApplication.class, args);
	}

}
