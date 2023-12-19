package com.contentgrid.surveyor.application.pegman.boot;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.DelegatingJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
class ContentgridSurveyorPegmanApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private ReactiveJwtAuthenticationConverter reactiveJwtAuthenticationConverter;

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
	void requiresOAuthEntitlementForReadingMeasurements_entitlementPresent() {
		rest.mutateWith(mockJwt()
						.jwt(jwtBuilder -> jwtBuilder.claim("entitlements", "surveyor:pegman:read"))
						.authorities(new DelegatingJwtGrantedAuthoritiesConverter(
								jwt -> reactiveJwtAuthenticationConverter.convert(jwt).block().getAuthorities()))
				).get()
				.uri("/metrics/storage:stored_bytes?start={start}&end={end}", Instant.now().minus(1, ChronoUnit.DAYS),
						Instant.now())
				.exchange()
				.expectStatus()
				.is2xxSuccessful();
	}

	@Test
	void requiresOAuthEntitlementForReadingMeasurements_entitlementMissing() {
		rest.mutateWith(mockJwt()
						.authorities(new DelegatingJwtGrantedAuthoritiesConverter(
								jwt -> reactiveJwtAuthenticationConverter.convert(jwt).block().getAuthorities()))
				).get()
				.uri("/metrics/storage:stored_bytes?start={start}&end={end}", Instant.now().minus(1, ChronoUnit.DAYS),
						Instant.now())
				.exchange()
				.expectStatus()
				.isForbidden();
	}

	@Test
	void requiresOAuthEntitlementForReadingMeasurements_jwtMissing() {
		rest.get()
				.uri("/metrics/storage:stored_bytes?start={start}&end={end}", Instant.now().minus(1, ChronoUnit.DAYS),
						Instant.now())
				.exchange()
				.expectStatus()
				.isUnauthorized();
	}
}
