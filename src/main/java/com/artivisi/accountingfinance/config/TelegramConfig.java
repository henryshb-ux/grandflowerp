package com.artivisi.accountingfinance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "telegram.bot")
@Getter
@Setter
public class TelegramConfig {

    private boolean enabled;
    private String token;
    private String username;
    private Webhook webhook = new Webhook();

    @Getter
    @Setter
    public static class Webhook {
        private String url;
        private String secretToken;
    }
}
