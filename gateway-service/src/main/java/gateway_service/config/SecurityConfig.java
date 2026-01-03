package gateway_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
		http
			.csrf(ServerHttpSecurity.CsrfSpec::disable)
			.authorizeExchange(exchange -> exchange
				// Health endpoint remains public
				.pathMatchers("/actuator/health").permitAll()
				// Protect gateway routes to downstream services
				.pathMatchers("/notification/**", "/customer/**").authenticated()
				// Allow everything else for now (can be tightened later)
				.anyExchange().permitAll()
			)
			// Enable JWT-based authentication using Keycloak-issued tokens
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

		return http.build();
	}
}