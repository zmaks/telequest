package com.mzheltoukhov.questpistols.configuration.telegram;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "telegram")
@Data
public class TelegramBotProperties {
    private String token;
    private String botName;
    private TelegramProxy proxy;
    private Integer threads;

    @Data
    public static class TelegramProxy {
        private Boolean enabled;
        private String host;
        private Integer port;
    }
}
