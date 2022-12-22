package com.mzheltoukhov.questpistols.configuration.telegram;

import com.mzheltoukhov.questpistols.bot.QuestPistolsBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;

@Configuration
public class TelegramBotConfiguration {

    @Bean
    public QuestPistolsBot questPistolsBot(TelegramBotProperties botProperties) {
        return new QuestPistolsBot(getBotOptions(botProperties), botProperties);
    }

    private DefaultBotOptions getBotOptions(TelegramBotProperties botProperties) {
        DefaultBotOptions options = new DefaultBotOptions();
        options.setMaxThreads(botProperties.getThreads());
        if (botProperties.getProxy().getEnabled()) {
            options.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);
            options.setProxyHost(botProperties.getProxy().getHost());
            options.setProxyPort(botProperties.getProxy().getPort());
        }
        return options;
    }
}
