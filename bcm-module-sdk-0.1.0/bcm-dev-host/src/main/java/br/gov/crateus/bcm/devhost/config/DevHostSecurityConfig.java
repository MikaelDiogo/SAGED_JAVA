package br.gov.crateus.bcm.devhost.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties(DevHostSecurityProperties.class)
@EnableMethodSecurity
public class DevHostSecurityConfig {

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(new RealmRoleAuthoritiesConverter());
		return converter;
	}

	@Bean
	@Order(1)
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			DevHostSecurityProperties securityProperties,
			JwtAuthenticationConverter jwtAuthenticationConverter
	) throws Exception {
		http
				.cors(cors -> {})
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> {
					auth.requestMatchers("/actuator/health", "/actuator/info").permitAll();
					auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
					auth.requestMatchers(HttpMethod.GET, "/api/v1/system/ping").permitAll();
					auth.requestMatchers(HttpMethod.GET, "/api/v1/*/hello").permitAll();
					// Module webhooks (Telegram/WhatsApp style) — authenticated by secret in the module.
					auth.requestMatchers(HttpMethod.POST, "/api/v1/*/webhooks/**").permitAll();
					// Telegram Mini App — authenticated by Telegram initData, not JWT.
					auth.requestMatchers("/telegram/app", "/telegram/app/**").permitAll();
					auth.requestMatchers("/api/v1/saged/telegram/app/**").permitAll();
					if (securityProperties.jwtEnabled()) {
						auth.anyRequest().authenticated();
					} else {
						auth.anyRequest().permitAll();
					}
				});

		if (securityProperties.jwtEnabled()) {
			http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
					jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
		}

		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOriginPattern("http://localhost:*");
		config.addAllowedOriginPattern("http://127.0.0.1:*");
		config.setAllowCredentials(true);
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
