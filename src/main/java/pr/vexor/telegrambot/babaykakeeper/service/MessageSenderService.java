package pr.vexor.telegrambot.babaykakeeper.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageId;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import pr.vexor.telegrambot.babaykakeeper.bot.BabaykaTelegramBot;
import pr.vexor.telegrambot.babaykakeeper.utils.TextFields;

@Slf4j
@Service
@AllArgsConstructor
public class MessageSenderService {

    @Value("${telegram.private-group.text:rtbrtb}")
    private final String linkText;
    
    private static final String MESSAGE_LINK_FORMAT = "https://t.me/c/%s/%s";
    
    private final BabaykaTelegramBot bot;
  
    /**
     * Копирование сообщения в закрытую группу друзей
     */
    public Long copyMessageToFriendsGroup(String friendsGroupId, Message originalPost) throws TelegramApiException {
        CopyMessage copyMessage = new CopyMessage();
        copyMessage.setChatId(friendsGroupId);
        copyMessage.setFromChatId(originalPost.getChatId().toString());
        copyMessage.setMessageId(originalPost.getMessageId());

        if (originalPost.getCaption() != null) {
            copyMessage.setCaption(originalPost.getCaption());
        }

        MessageId messageId = bot.execute(copyMessage);
        log.info("Message copied to friends group: {}", messageId.getMessageId());
        return messageId.getMessageId();
    }

    /**
     * Публикация поста в канал с добавлением ссылки на приватное обсуждение
     */
    public Integer sendPostToChannel(String channelId, Message original, String discussionLink) throws TelegramApiException {
    String linkHtml = String.format(TextFields.BUTTON_LINK_HTML_FORMAT, linkText, discussionLink);

    if (original.hasText()) {
        String fullText = original.getText() + linkHtml;
        SendMessage message = new SendMessage();
        message.setChatId(channelId);
        message.setText(fullText);
        message.setParseMode("HTML");
        Message sentMessage = bot.execute(message);
        return sentMessage.getMessageId();
    } else if (original.hasPhoto()) {
        var photos = original.getPhoto();
        String fileId = photos.get(photos.size() - 1).getFileId();
        String caption = (original.getCaption() != null ? original.getCaption() : "") + linkHtml;

        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(channelId);
        sendPhoto.setPhoto(new InputFile(fileId));
        sendPhoto.setCaption(caption);
        sendPhoto.setParseMode("HTML");
        Message sentMessage = bot.execute(sendPhoto);
        return sentMessage.getMessageId();
    } else if (original.hasVideo()) {
        String fileId = original.getVideo().getFileId();
        String caption = (original.getCaption() != null ? original.getCaption() : "") + linkHtml;

        SendVideo sendVideo = new SendVideo();
        sendVideo.setChatId(channelId);
        sendVideo.setVideo(new InputFile(fileId));
        sendVideo.setCaption(caption);
        sendVideo.setParseMode("HTML");
        Message sentMessage = bot.execute(sendVideo);
        return sentMessage.getMessageId();
    } else if (original.getDocument() != null) {
        String fileId = original.getDocument().getFileId();
        String caption = (original.getCaption() != null ? original.getCaption() : "") + linkHtml;

        SendDocument sendDoc = new SendDocument();
        sendDoc.setChatId(channelId);
        sendDoc.setDocument(new InputFile(fileId));
        sendDoc.setCaption(caption);
        sendDoc.setParseMode("HTML");
        Message sentMessage = bot.execute(sendDoc);
        return sentMessage.getMessageId();
    } else {
        SendMessage msg = new SendMessage();
        msg.setChatId(channelId);
        msg.setText("Новое сообщение\n\n" + linkHtml);
        msg.setParseMode("HTML");
        Message sentMessage = bot.execute(msg);
        return sentMessage.getMessageId();
    }
}

    /**
     * Создание ссылки на сообщение в чате
     */
    public String createMessageLink(String chatId, Long messageId) {
        String cleanChatId = chatId.startsWith("-100") ? chatId.substring(4) : chatId;
        return String.format(MESSAGE_LINK_FORMAT, cleanChatId, messageId);
    }
    
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