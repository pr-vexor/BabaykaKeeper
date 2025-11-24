package pr.vexor.telegrambot.babaykakeeper.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageId;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;

import pr.vexor.telegrambot.babaykakeeper.bot.BabaykaTelegramBot;
import pr.vexor.telegrambot.babaykakeeper.config.TelegramProperties;
import pr.vexor.telegrambot.babaykakeeper.utils.TextFields;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@AllArgsConstructor
public class MessageSenderService {
        
    private final BabaykaTelegramBot bot;
    private final TelegramProperties telegramProperties;

    /**
     * Публикация поста в канал с добавлением ссылки на приватное обсуждение
     */
    public Integer sendSingleMessagePostToChannel(String channelId, Message original, String discussionLink) throws TelegramApiException {
        if (original.hasText()) {
            String fullText = addLinkTagAfterOtherTags(original.getText(), discussionLink);
            SendMessage message = new SendMessage();
            message.setChatId(channelId);
            message.setText(fullText);
            message.setParseMode("HTML");
            Message sentMessage = bot.execute(message);
            return sentMessage.getMessageId();
        } else if (original.hasPhoto()) {
            var photos = original.getPhoto();
            String fileId = photos.get(photos.size() - 1).getFileId();
            String caption = addLinkTagAfterOtherTags(original.getCaption(), discussionLink);
            
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(channelId);
            sendPhoto.setPhoto(new InputFile(fileId));
            sendPhoto.setCaption(caption);
            sendPhoto.setParseMode("HTML");
            Message sentMessage = bot.execute(sendPhoto);
            return sentMessage.getMessageId();
        } else if (original.hasVideo()) {
            String fileId = original.getVideo().getFileId();
            String caption = addLinkTagAfterOtherTags(original.getCaption(), discussionLink);

            SendVideo sendVideo = new SendVideo();
            sendVideo.setChatId(channelId);
            sendVideo.setVideo(new InputFile(fileId));
            sendVideo.setCaption(caption);
            sendVideo.setParseMode("HTML");
            Message sentMessage = bot.execute(sendVideo);
            return sentMessage.getMessageId();
        } else if (original.getDocument() != null) {
            String fileId = original.getDocument().getFileId();
            String caption = addLinkTagAfterOtherTags(original.getCaption(), discussionLink);

            SendDocument sendDoc = new SendDocument();
            sendDoc.setChatId(channelId);
            sendDoc.setDocument(new InputFile(fileId));
            sendDoc.setCaption(caption);
            sendDoc.setParseMode("HTML");
            Message sentMessage = bot.execute(sendDoc);
            return sentMessage.getMessageId();
        } else {
            String fullText = addLinkTagAfterOtherTags("Новое сообщение", discussionLink);
            SendMessage msg = new SendMessage();
            msg.setChatId(channelId);
            msg.setText(fullText);
            msg.setParseMode("HTML");
            Message sentMessage = bot.execute(msg);
            return sentMessage.getMessageId();
        }
    }

    /**
     * Публикация альбома в канал с добавлением ссылки на приватное обсуждение
     */
    public List<Message> sendAlbumToChannel(String channelId, String mediaGroupId, List<InputMedia> mediaList, String discussionLink) throws TelegramApiException {
        // Добавляем ссылку на закрытую группу к первому фото
        String linkHtml = String.format(TextFields.BUTTON_LINK_HTML_FORMAT, discussionLink, telegramProperties.getPrivateGroup().getText());
        String captionWithLink = (mediaList.get(0).getCaption() != null ? mediaList.get(0).getCaption() : "") + linkHtml;
        
        if (!mediaList.isEmpty()) {
            mediaList.get(0).setCaption(captionWithLink);
            mediaList.get(0).setParseMode("HTML");
        }

        SendMediaGroup sendMediaGroup = new SendMediaGroup();
        sendMediaGroup.setChatId(channelId);
        sendMediaGroup.setMedias(mediaList);

        return bot.execute(sendMediaGroup);
    }
    
    /**
     * Копирование одиночного сообщения в закрытую группу
     */
    public Long copySingleMessagePostToPrivateGroup(String privateGroupId, Message originalPost) throws TelegramApiException {
        CopyMessage copyMessage = new CopyMessage();
        copyMessage.setChatId(privateGroupId);
        copyMessage.setFromChatId(originalPost.getChatId().toString());
        copyMessage.setMessageId(originalPost.getMessageId());

        // Подпись к альбомам
        if (originalPost.getCaption() != null) {
            copyMessage.setCaption(originalPost.getCaption());
        }

        MessageId messageId = bot.execute(copyMessage);
        log.info("Single message copied to private group: {}", messageId.getMessageId());
        return messageId.getMessageId();
    }
    
    /**
    * Отправка альбома в закрытую группу
    */
    public List<Message> copyAlbumToPrivateGroup(String privateGroupId, List<InputMedia> mediaList, String caption) throws TelegramApiException {
        // Добавляем подпись к первому фото
        if (!mediaList.isEmpty() && caption != null) {
            mediaList.get(0).setCaption(caption);
            mediaList.get(0).setParseMode("HTML");
        }

        SendMediaGroup sendMediaGroup = new SendMediaGroup();
        sendMediaGroup.setChatId(privateGroupId);
        sendMediaGroup.setMedias(mediaList);

        return bot.execute(sendMediaGroup);
    }
    
    /**
     * Создание ссылки на сообщение в чате
     */
    public String createMessageLink(String chatId, Long messageId) {
        String cleanChatId = chatId.startsWith("-100") ? chatId.substring(4) : chatId;
        return String.format(TextFields.MESSAGE_LINK_FORMAT, cleanChatId, messageId);
    }
    
    /**
     * Отправка сообщения
     */
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
    
    private String addLinkTagAfterOtherTags(String text, String discussionLink) {
        String lineSeparator = "\n\n";

        // если текста нет совсем, сразу вставляем теги
        if (text == null || text.isEmpty()) {
            log.error("Text is empty or null");
            text = "";
        // если уже есть сепарированные от текста теги в конце,
        } else {
            String regex = "(.+\\n\\n)(#\\w+(\\s+#\\w+)*)$";
            Pattern pattern = Pattern.compile(regex, Pattern.DOTALL); // для многострочности
            Matcher matcher = pattern.matcher(text.trim());
            if (matcher.matches()) {
                lineSeparator = " ";
            }
        }
        
        // Добавляем ссылку на закрытую группу к первому фото
        String linkHtml = String.format(TextFields.BUTTON_LINK_HTML_FORMAT, discussionLink, telegramProperties.getPrivateGroup().getText());
        text = text + lineSeparator + linkHtml;
        
        return text;
    }
}