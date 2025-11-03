package pr.vexor.telegrambot.babaykakeeper.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

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

    @Value("${telegram.owner-id}")
    private Long ownerId;
        
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

        if (!activityManager.isActive() && !isActivateOrStartCommand(update)) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                commandHandler.handleButIgnoreCommand(update.getMessage().getChatId());
            }
            log.info("Bot is in standby mode, ignoring update");
            return;
        }

        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();

                if (isPrivateChat(message)) {
                    if (isAdmin(message.getFrom())) {
                        if (message.hasText() && message.getText().startsWith("/")) {
                            commandHandler.handleCommand(message);
                        } else {
                            // Это пост для публикации в канале
                            channelService.publishPostViaBot(message);
                        }
                    } else {
                        // Не админ — обрабатываем только команды
                        if (message.hasText() && message.getText().startsWith("/")) {
                            commandHandler.handleCommand(message);
                        }
                        // Иначе — игнорируем
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing update: {}", e.getMessage(), e);
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
    
    private boolean isPrivateChat(Message message) {
        return "private".equals(message.getChat().getType());
    }
    
    private boolean isAdmin(User user) {
        return user.getId().equals(ownerId);
    }
}