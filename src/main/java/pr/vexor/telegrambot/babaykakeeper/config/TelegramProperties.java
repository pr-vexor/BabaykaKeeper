package pr.vexor.telegrambot.babaykakeeper.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import pr.vexor.telegrambot.babaykakeeper.bot.BabaykaTelegramBot;

@Slf4j
@Configuration
@Data
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {

    private Bot bot;
    private int albumTimeoutSeconds;
    private String ownerId;
    private String channelId;
    private PrivateGroup privateGroup;

    @Data
    public static class Bot {
        private String token;
        private String name;
    }
    
    @Data
    public static class PrivateGroup {
        private String id;
        private String text;
    }
    
    @Bean
    public TelegramBotsApi telegramBotsApi(BabaykaTelegramBot bot) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);
        log.info("Bot {} was registered successfully", bot.getBotUsername());
        return botsApi;
    }
    
}