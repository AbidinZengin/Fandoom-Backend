package com.example.fandoom_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"MAIL_HOST=localhost",
		"MAIL_USERNAME=test",
		"MAIL_PASSWORD=test"
})
class FandoomBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
