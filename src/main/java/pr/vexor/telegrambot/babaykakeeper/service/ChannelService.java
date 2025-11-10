package pr.vexor.telegrambot.babaykakeeper.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import pr.vexor.telegrambot.babaykakeeper.config.ApplicationProperties;
import pr.vexor.telegrambot.babaykakeeper.config.TelegramProperties;
import pr.vexor.telegrambot.babaykakeeper.model.Post;
import pr.vexor.telegrambot.babaykakeeper.repository.ProcessedPostRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@AllArgsConstructor
public class ChannelService {

    private final Map<String, List<InputMedia>> mediaGroups = new ConcurrentHashMap<>();

    private final MessageSenderService messageSender;
    private final ProcessedPostRepository postRepository;
    private final TelegramProperties telegramProperties;
    private final ApplicationProperties applicationProperties;
    
    /**
     * Проверка существования поста в БД
     */
    public boolean isPostProcessed(Integer messageId) {
        return postRepository.existsById(messageId);
    }

    /**
     * Публикация поста в канал через бота (админ присылает пост в ЛС)
     */
    public void publishPostViaBot(Message originalMessage) {
        Integer tempId = originalMessage.getMessageId();
        String channelId = telegramProperties.getChannelId();
        String privateGroupId = telegramProperties.getPrivateGroup().getId();

        try {
            // Копируем пост в закрытую группу друзей
            Long privateMessageId = messageSender.copyMessageToFriendsGroup(privateGroupId, originalMessage);
            log.info("Post copied to private group, privateMessageId: {}", privateMessageId);

            // Создаем ссылку
            String discussionLink = messageSender.createMessageLink(privateGroupId, privateMessageId);

            // является ли сообщение альбомом
            if (originalMessage.getMediaGroupId() != null) {
                handleAlbum(originalMessage, channelId, discussionLink, privateMessageId);
            } else {
                // Обработка одиночного сообщения
                Integer channelMessageId = messageSender.sendPostToChannel(channelId, originalMessage, discussionLink);

                Post post = new Post(channelMessageId, Long.valueOf(channelId), 
                                     applicationProperties.getName(), privateMessageId);
                postRepository.save(post);

                log.info("Post successfully published to channel via bot and saved to DB");
            }
        } catch (Exception e) {
            log.error("Error publishing post via bot (tempId: {}): {}", tempId, e.getMessage(), e);
        }
    }

    /**
     * Обработка альбома
     */
    private void handleAlbum(Message message, String channelId, String discussionLink, Long privateMessageId) throws TelegramApiException {
        String mediaGroupId = message.getMediaGroupId();
        if (mediaGroupId == null) {
            log.warn("Message is not part of an album, chatId: {}", message.getChatId());
            return;
        }

        // Собираем медиа-файлы
        List<PhotoSize> photos = message.getPhoto();
        PhotoSize largestPhoto = photos.get(photos.size() - 1);

        InputMedia inputMedia = new InputMediaPhoto();
        inputMedia.setMedia(largestPhoto.getFileId());

        // Добавляем медиа в группу
        mediaGroups.computeIfAbsent(mediaGroupId, k -> new ArrayList<>()).add(inputMedia);

        // Если это последнее фото в альбоме, отправляем весь альбом
        if (isLastPhotoInAlbum(message)) {
            List<InputMedia> mediaList = mediaGroups.remove(mediaGroupId); // Получаем медиа-файлы из группы
            if (mediaList == null || mediaList.isEmpty()) {
                log.warn("No media found for group ID: {}", mediaGroupId);
                return;
            }

            List<Message> channelMessages = messageSender.sendAlbumToChannel(channelId, mediaGroupId, mediaList, discussionLink);

            // Сохраняем первый messageId из альбома
            Integer channelMessageId = channelMessages.get(0).getMessageId();
            Post post = new Post(channelMessageId, Long.valueOf(channelId), applicationProperties.getName(), privateMessageId);
            postRepository.save(post);

            log.info("Album with {} photos sent successfully, channelMessageId: {}", mediaList.size(), channelMessageId);
        }
    }

    /**
     * Проверка, является ли сообщение последним в альбоме
     */
    private boolean isLastPhotoInAlbum(Message message) {
        // Упрощённая проверка: если количество медиа достигло предела (например, 10 фото)
        return mediaGroups.get(message.getMediaGroupId()).size() >= 10;
    }

    /**
     * Сбор медиа-файлов из альбома
     */
    private List<InputMedia> collectMediaFromAlbum(Message message) {
        List<InputMedia> mediaList = new ArrayList<>();
        if (message.hasPhoto()) {
            List<PhotoSize> photos = message.getPhoto();
            PhotoSize largestPhoto = photos.get(photos.size() - 1); // Берём самое большое изображение

            InputMedia inputMedia = new InputMediaPhoto();
            inputMedia.setMedia(largestPhoto.getFileId());
            mediaList.add(inputMedia);
        }
        // TODO: Добавить поддержку других типов медиа (например, видео)
        return mediaList;
    }
}