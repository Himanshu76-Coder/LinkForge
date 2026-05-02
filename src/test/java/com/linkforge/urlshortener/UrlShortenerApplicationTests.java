package com.linkforge.urlshortener;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Verifies the Spring application context loads successfully with test configuration
@SpringBootTest
@ActiveProfiles("test")
class UrlShortenerApplicationTests {

	@Test
	void contextLoads() {
	}

}
