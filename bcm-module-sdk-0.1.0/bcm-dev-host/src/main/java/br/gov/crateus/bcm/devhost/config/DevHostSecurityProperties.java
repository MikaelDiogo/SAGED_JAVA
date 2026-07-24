package br.gov.crateus.bcm.devhost.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bcm.security")
public record DevHostSecurityProperties(boolean jwtEnabled) {
}
