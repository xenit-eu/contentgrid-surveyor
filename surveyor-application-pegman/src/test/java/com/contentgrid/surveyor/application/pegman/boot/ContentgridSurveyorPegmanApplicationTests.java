package com.contentgrid.surveyor.application.pegman.boot;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
class ContentgridSurveyorPegmanApplicationTests {

	@Autowired
	private ApplicationContext context;

	WebTestClient rest;

	@BeforeEach
	public void setup() {
		this.rest = WebTestClient
				.bindToApplicationContext(this.context)
				// add Spring Security test Support
				.apply(springSecurity())
				.configureClient()
				.build();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void requiresOAuthScopeForReadingMeasurements_scopePresent() {
		rest.mutateWith(mockJwt()
						.jwt(jwtBuilder -> jwtBuilder.claim("scope", "surveyor:pegman:read"))
				).get()
				.uri("/metrics/storage:stored_bytes?start={start}&end={end}", Instant.now().minus(1, ChronoUnit.DAYS),
						Instant.now())
				.exchange()
				.expectStatus()
				.is2xxSuccessful();
	}

	@Test
	void requiresOAuthScopeForReadingMeasurements_scopeMissing() {
		rest.mutateWith(mockJwt()).get()
				.uri("/metrics/storage:stored_bytes?start={start}&end={end}", Instant.now().minus(1, ChronoUnit.DAYS),
						Instant.now())
				.exchange()
				.expectStatus()
				.isForbidden();
	}

	@Test
	void requiresOAuthScopeForReadingMeasurements_jwtMissing() {
		rest.get()
				.uri("/metrics/storage:stored_bytes?start={start}&end={end}", Instant.now().minus(1, ChronoUnit.DAYS),
						Instant.now())
				.exchange()
				.expectStatus()
				.isUnauthorized();
	}
}
