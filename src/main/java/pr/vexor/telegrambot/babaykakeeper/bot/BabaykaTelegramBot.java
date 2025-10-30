package pr.vexor.telegrambot.babaykakeeper.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import pr.vexor.telegrambot.babaykakeeper.service.BotActivityManager;
import pr.vexor.telegrambot.babaykakeeper.service.ChannelService;
import pr.vexor.telegrambot.babaykakeeper.service.CommandHandler;

@Slf4j
@Service
public class BabaykaTelegramBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.token}")
    private String botToken;
    
    @Value("${telegram.bot.name}") 
    private String botName;

    @Lazy
    @Autowired
    private CommandHandler commandHandler;
    
    @Lazy
    @Autowired
    private ChannelService channelService;
    
    @Autowired
    private BotActivityManager activityManager;
    
    @Override
    public void onUpdateReceived(Update update) {
        log.info("Update was found, updateId: {}", update.getUpdateId());

        // Проверяем активность бота (кроме команды activate)
        if (!activityManager.isActive() && !isActivateOrStartCommand(update)) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                commandHandler.handleButIgnoreCommand(update.getMessage().getChatId());
            }
            log.info("Bot is in standby mode, ignoring update");
            return;
        }

        try {
            // сообщение
            if (update.hasMessage() && update.getMessage().hasText()) {
                commandHandler.handleCommand(update.getMessage());
            }
            // пост
            if (update.hasChannelPost()) {
                channelService.processChannelPost(update.getChannelPost());
            }
        } catch (Exception e) {
            log.error("Error processing update: {}", e.getMessage());
        }
    }
    
    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
    
    private boolean isActivateOrStartCommand(Update update) {
        return update.hasMessage() && 
               update.getMessage().hasText() && 
               (update.getMessage().getText().equals("/activate") ||
                update.getMessage().getText().equals("/start"));
    }

}