package pr.vexor.telegrambot.babaykakeeper.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import pr.vexor.telegrambot.babaykakeeper.bot.BabaykaTelegramBot;

@Slf4j
@Service
@AllArgsConstructor
public class MessageSenderService {

    private final BabaykaTelegramBot bot;

    public void sendMessage(Long chatId, String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(text);
            message.setParseMode("HTML");
            
            bot.execute(message);
            log.info("Message was sent into chat, chatId: {}", chatId);
        } catch (TelegramApiException e) {
            log.error("Message send error: {}", e.getMessage());
        }
    }
}