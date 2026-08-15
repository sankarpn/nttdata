package com.nttdata.documentqa;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.ai.openai.api-key=test-key")
class DocumentQaServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
