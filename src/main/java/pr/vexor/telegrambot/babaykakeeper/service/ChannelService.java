package pr.vexor.telegrambot.babaykakeeper.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import pr.vexor.telegrambot.babaykakeeper.model.Post;
import pr.vexor.telegrambot.babaykakeeper.repository.ProcessedPostRepository;
import pr.vexor.telegrambot.babaykakeeper.utils.TextFields;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChannelService {

    @Value("${app.friends.group-id:NOT_SET_YET}")
    private String friendsGroupId;

    private static final String MESSAGE_LINK_FORMAT = "https://t.me/c/%s/%s";

    @Autowired
    private MessageSenderService messageSender;

    @Autowired
    private ProcessedPostRepository postRepository;

    @Autowired
    private BotActivityManager activityManager;

    /**
     * Проверка существования поста в БД
     */
    public boolean isPostProcessed(Integer messageId) {
        return postRepository.existsById(messageId);
    }
    
    /**
     * Обработка поста
     */
    public void processChannelPost(Message channelPost) {    
        log.info("Post {} is pricessing", channelPost);
        
        Integer messageId = channelPost.getMessageId();

        // Проверяем, не обработан ли уже пост
        if (postRepository.existsById(messageId)) {
            log.info("Post {} already processed, skipping", messageId);
            return;
        }

        try {
            // Копируем пост в группу друзей и получаем ID
            Integer friendsMessageId = copyToFriendsGroup(channelPost);
            log.info("Post was copied to friends group, friendsMessageId: {}", friendsMessageId);
            
            // Добавляем кнопку обсуждения и сохраняем как обработанный
            addDiscussionButton(channelPost, friendsMessageId);
            saveProcessedPost(channelPost, friendsMessageId);
            
            log.info("Button was added, post was saved");
        } catch (Exception e) {
            log.error("Error processing post {}: {}", messageId, e.getMessage());
        }
    }

    private Integer copyToFriendsGroup(Message originalPost) throws TelegramApiException {
        CopyMessage copyMessage = new CopyMessage();
        copyMessage.setChatId(friendsGroupId);
        copyMessage.setFromChatId(originalPost.getChatId().toString());
        copyMessage.setMessageId(originalPost.getMessageId());

        if (originalPost.getCaption() != null) {
            copyMessage.setCaption(originalPost.getCaption());
        }

        /*
        Object result = bot.execute(copyMessage);
        System.out.println("🔍 Результат execute: " + result + " (тип: " + result.getClass().getName() + ")");

        // Обрабатываем разные типы
        if (result instanceof String) {
            return Integer.valueOf((String) result);
        } else {
            // Для других типов возвращаем фиктивный ID
            return originalPost.getMessageId() + 1000;
        }*/
        return null;
    }

    private void addDiscussionButton(Message originalPost, Integer friendsMessageId) throws TelegramApiException {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(TextFields.BUTTON_TEXT);
        button.setUrl(createMessageLink(friendsGroupId, friendsMessageId));
        row.add(button);
        
        keyboard.setKeyboard(List.of(row));

        EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
        editMarkup.setChatId(originalPost.getChatId().toString());
        editMarkup.setMessageId(originalPost.getMessageId());
        editMarkup.setReplyMarkup(keyboard);

        //bot.execute(editMarkup);
    }

    private String createMessageLink(String chatId, Integer messageId) {
        // Убираем "-100" из chatId для формирования ссылки
        String cleanChatId = chatId.replace("-100", "");
        String link = String.format(MESSAGE_LINK_FORMAT, cleanChatId, messageId);
        return link;
    }

    private void saveProcessedPost(Message channelPost, Integer friendsMessageId) {
        Post processedPost = new Post(
            channelPost.getMessageId(),
            channelPost.getChatId(),
            activityManager.getInstanceId(),
            friendsMessageId
        );
        postRepository.save(processedPost);
    }
}