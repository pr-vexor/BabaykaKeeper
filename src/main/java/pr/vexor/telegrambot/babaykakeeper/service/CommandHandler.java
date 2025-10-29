package pr.vexor.telegrambot.babaykakeeper.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

import pr.vexor.telegrambot.babaykakeeper.utils.TextFields;

@Slf4j
@Service
@AllArgsConstructor
public class CommandHandler {

    private final MessageSenderService messageSender;
    private final BotActivityManager activityManager;

    public void handleCommand(Message message) {
        String text = message.getText();
        Long chatId = message.getChatId();
        String userName = message.getFrom().getUserName();

        log.info("Processing command from @{}: {}", userName, text);

        switch (text) {
            case "/start":
                handleStart(chatId);
                break;
            case "/getchatid":
                handleGetChatId(message);
                break;
            case "/activate":
                handleActivate(chatId, userName);
                break;
            case "/standby":
                handleStandby(chatId, userName);
                break;
            case "/status":
                handleStatus(chatId);
                break;
            default:
                handleUnknownCommand(chatId);
                break;
        }
    }

    private void handleStart(Long chatId) {
        messageSender.sendMessage(chatId, TextFields.START_COMMAND_TEXT);
        log.info("Start command");
    }

    private void handleGetChatId(Message message) {
        String chatTitle = message.getChat().getTitle();
        String chatInfo = String.format(TextFields.CHAT_INFO_COMMAND_TEXT,
                message.getChatId(),
                message.getChat().getType(),
                chatTitle != null ? chatTitle : TextFields.MESSAGES_TYPE
        );
        messageSender.sendMessage(message.getChatId(), chatInfo);
        log.info("ChatId was shown: {}", message.getChatId());
    }

    private void handleActivate(Long chatId, String userName) {
        if (activityManager.activate()) {
            messageSender.sendMessage(chatId, TextFields.ACTIVATION_SUCCESS);
            log.info("Bot was activated by user @{}", userName);
        } else {
            messageSender.sendMessage(chatId, TextFields.ACTIVATION_ERORR);
        }
    }

    private void handleStandby(Long chatId, String userName) {
        if (activityManager.deactivate()) {
            messageSender.sendMessage(chatId, TextFields.STANDBYING_SUCCESS);
            log.info("Bot was deactivated by user @{}", userName);
        } else {
            messageSender.sendMessage(chatId, TextFields.STANDBYING_ERORR);
        }
    }

    private void handleStatus(Long chatId) {
        String status = activityManager.isActive() ? TextFields.ACTIVE_STATUS : TextFields.STANDBY_STATUS;
        String messageText = String.format(TextFields.STATUS_COMMAND_TEXT, 
                status, 
                activityManager.getInstanceId());
        messageSender.sendMessage(chatId, messageText);
    }

    private void handleUnknownCommand(Long chatId) {
        messageSender.sendMessage(chatId, TextFields.UNKNOWN_COMMAND_TEXT);
    }
}