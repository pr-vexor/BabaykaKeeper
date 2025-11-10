package pr.vexor.telegrambot.babaykakeeper.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import pr.vexor.telegrambot.babaykakeeper.config.TelegramProperties;
import pr.vexor.telegrambot.babaykakeeper.service.BotActivityManager;
import pr.vexor.telegrambot.babaykakeeper.service.ChannelService;
import pr.vexor.telegrambot.babaykakeeper.service.CommandHandler;

import java.util.Objects;

@Slf4j
@Service
public class BabaykaTelegramBot extends TelegramLongPollingBot {
        
    @Lazy
    @Autowired
    private CommandHandler commandHandler;
    
    @Lazy
    @Autowired
    private ChannelService channelService;
    
    @Autowired
    private BotActivityManager activityManager;
    
    @Autowired
    private TelegramProperties telegramProperties;
    
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
                Long chatId = message.getChatId();

                // Обработка команд
                if (message.hasText() && message.getText().startsWith("/")) {
                    if (isAdmin(message)) {
                        commandHandler.handleCommand(message);
                    } else {
                        commandHandler.handleButIgnoreNonAdminCommand(chatId);
                        log.info("Non-admin user tried to send a command, chatId: {}", chatId);
                    }
                // Публикация постов (только из приватного чата)
                } else if (isPrivateChat(message)) {
                    if (isAdmin(message)) {
                        channelService.publishPostViaBot(message);
                    } else {
                        commandHandler.handleButIgnoreNonAdminCommand(chatId);
                        log.info("Non-admin user tried to publish a post, chatId: {}", chatId);
                    }
                } else {
                    log.info("Post publication attempt from non-private chat, chatId: {}", chatId);
                }
            }
        } catch (Exception e) {
            log.error("Error processing update: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public String getBotUsername() {
        return telegramProperties.getBot().getName();
    }

    @Override
    public String getBotToken() {
        return telegramProperties.getBot().getToken();
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
    
    private boolean isAdmin(Message message) {
        User user = message.getFrom();
        Chat senderChat = message.getSenderChat();

        // От имени группы (GroupAnonymousBot)
        if (senderChat != null && "supergroup".equals(senderChat.getType())) {
            String senderChatId = senderChat.getId().toString();
            String propertiesChatId = telegramProperties.getPrivateGroup().getId();
            return Objects.equals(senderChatId, propertiesChatId);
        }

        // Обычный пользователь
        if (user != null) {
            Long ownerId = Long.valueOf(telegramProperties.getOwnerId());
            return user.getId().equals(ownerId);
        }

        return false;
    }
}