package br.gov.crateus.bcm.saged.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SagedTelegramProperties.class)
public class SagedSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain telegramWebhookChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/api/v1/saged/telegram/webhook")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .build();
    }
}
