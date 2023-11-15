package com.contentgrid.surveyor.application.surveyor.boot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"surveyor.resource-linkage.captain.url=none"
})
class ContentgridSurveyorApplicationTests {

	@Test
	void contextLoads() {
	}

}
