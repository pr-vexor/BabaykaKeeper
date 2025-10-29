package pr.vexor.telegrambot.babaykakeeper.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import pr.vexor.telegrambot.babaykakeeper.bot.BabaykaTelegramBot;

@Slf4j
@Configuration
public class BotConfig {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.name}")
    private String botName;

    @Bean
    public TelegramBotsApi telegramBotsApi(BabaykaTelegramBot bot) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);
        log.info("Bot {} was registered successfully", botName);
        return botsApi;
    }
    
}