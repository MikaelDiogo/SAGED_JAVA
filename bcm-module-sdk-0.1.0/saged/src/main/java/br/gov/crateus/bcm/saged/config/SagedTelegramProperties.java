package br.gov.crateus.bcm.saged.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "saged.telegram")
public class SagedTelegramProperties {

    private String botToken;
    private String webhookSecret;

    public String getBotToken() { return botToken; }
    public void setBotToken(String botToken) { this.botToken = botToken; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
}
