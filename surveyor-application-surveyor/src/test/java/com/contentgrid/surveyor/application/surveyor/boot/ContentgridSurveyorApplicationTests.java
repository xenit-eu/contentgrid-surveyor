package com.contentgrid.surveyor.application.surveyor.boot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;

@SpringBootTest(properties = {
		"surveyor.resource-linkage.captain.url=none"
}, classes = TestContentgridSurveyorApplication.class)
class ContentgridSurveyorApplicationTests {

	@MockBean
	ReactiveClientRegistrationRepository clientRegistrationRepository;

	@MockBean
	ReactiveOAuth2AuthorizedClientService reactiveOAuth2AuthorizedClientService;

	@Test
	void contextLoads() {
	}

}
