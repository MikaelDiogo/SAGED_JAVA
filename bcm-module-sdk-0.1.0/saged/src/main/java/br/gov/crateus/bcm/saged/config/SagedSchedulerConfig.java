package br.gov.crateus.bcm.saged.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(SagedTelegramProperties.class)
public class SagedSchedulerConfig {
}
