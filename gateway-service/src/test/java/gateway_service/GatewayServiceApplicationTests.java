package gateway_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayServiceApplicationTests {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void contextLoads() {
	}

	@Test
	void healthEndpointIsPublicWithoutAuthentication() {
		webTestClient.get()
			.uri("/actuator/health")
			.exchange()
			.expectStatus().isOk();
	}

	@Test
	void customerEndpointWithoutTokenIsUnauthorized() {
		webTestClient.get()
			.uri("/customer/test")
			.exchange()
			.expectStatus().isUnauthorized();
	}

	@Test
	void customerEndpointWithValidJwtIsAuthorized() {
		webTestClient.get()
			.uri("/customer/test")
			.header(HttpHeaders.AUTHORIZATION, "Bearer dummy-token")
			.exchange()
			.expectStatus().isOk()
			.expectBody(String.class).isEqualTo("customer-ok");
	}

	@Test
	void unsecuredEndpointIsAccessibleWithoutAuthentication() {
		webTestClient.get()
			.uri("/unsecured")
			.exchange()
			.expectStatus().isOk()
			.expectBody(String.class).isEqualTo("unsecured-ok");
	}

	@TestConfiguration
	static class TestSecurityConfig {

		@Bean
		ReactiveJwtDecoder reactiveJwtDecoder() {
			// Stub decoder that treats any bearer token as a valid JWT for testing purposes.
			return token -> Mono.just(
				Jwt.withTokenValue(token)
					.header("alg", "none")
					.claim("sub", "test-user")
					.build()
			);
		}

		@RestController
		static class TestCustomerController {

			@GetMapping("/customer/test")
			public String customerTest() {
				return "customer-ok";
			}
		}

		@RestController
		static class TestUnsecuredController {

			@GetMapping("/unsecured")
			public String unsecured() {
				return "unsecured-ok";
			}
		}
	}
}
