package com.blog_plateform;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BlogPlatformApiApplicationTests {

	@Disabled("needs full application context with DB and Redis — covered by integration tests")
	@Test
	void contextLoads() {
	}

}
