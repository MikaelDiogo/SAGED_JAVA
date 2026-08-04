package br.gov.crateus.bcm.saged.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableConfigurationProperties(SagedTelegramProperties.class)
public class SagedSecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain telegramPublicChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher(
                "/api/v1/saged/telegram/webhook",
                "/telegram/app", "/telegram/app/**",
                "/telegram/info", "/telegram/info/**",
                "/api/v1/saged/telegram/app", "/api/v1/saged/telegram/app/**"
            )
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .headers(headers -> headers
                .contentTypeOptions(cto -> {})
                .frameOptions(fo -> fo.deny())
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                .referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .addHeaderWriter((req, res) -> res.setHeader("X-Content-Type-Options", "nosniff"))
            )
            .build();
    }
}
