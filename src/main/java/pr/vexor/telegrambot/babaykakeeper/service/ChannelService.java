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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@AllArgsConstructor
public class ChannelService {

    private final Map<String, List<InputMedia>> mediaGroups = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, Set<String>> mediaGroupUniqueIds = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> mediaGroupTimeouts = new ConcurrentHashMap<>();

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
     * Публикация поста через бота (админ присылает пост в ЛС)
     */
    public void publishPostViaBot(Message originalMessage) {
        Integer tempId = originalMessage.getMessageId();
        String channelId = telegramProperties.getChannelId();

        try {
            // является ли сообщение альбомом
            if (originalMessage.getMediaGroupId() != null) {
                handleAlbum(originalMessage, channelId);
            } else {
                // Обработка одиночного сообщения
                String privateGroupId = telegramProperties.getPrivateGroup().getId();
                Long privateMessageId = messageSender.copySingleMessageToFriendsGroup(privateGroupId, originalMessage);
                log.info("Post copied to private group, privateMessageId: {}", privateMessageId);

                // Создаем ссылку
                String discussionLink = messageSender.createMessageLink(privateGroupId, privateMessageId);

                // Отправляем одиночное сообщение в канал
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
     * Обработка части альбома
     */
    private void handleAlbum(Message message, String channelId) {
        String mediaGroupId = message.getMediaGroupId();
        if (mediaGroupId == null) {
            log.warn("Message is not part of an album, chatId: {}", message.getChatId());
            return;
        }

        // Извлекаем file_unique_id самого большого изображения
        List<PhotoSize> photos = message.getPhoto();
        PhotoSize largestPhoto = photos.get(photos.size() - 1); // Берём самое большое изображение
        String fileUniqueId = largestPhoto.getFileUniqueId();

        // Создаем InputMedia для отправки
        InputMedia inputMedia = new InputMediaPhoto();
        inputMedia.setMedia(largestPhoto.getFileId());

        // Добавляем медиа в группу
        mediaGroups.computeIfAbsent(mediaGroupId, k -> new ArrayList<>()).add(inputMedia);

        // Отслеживаем уникальные file_unique_id
        mediaGroupUniqueIds.computeIfAbsent(mediaGroupId, k -> new HashSet<>()).add(fileUniqueId);

        // Отменяем предыдущий таймер, если он был
        ScheduledFuture<?> timeoutTask = mediaGroupTimeouts.get(mediaGroupId);
        if (timeoutTask != null) {
            timeoutTask.cancel(false);
        }

        // Устанавливаем новый таймер
        ScheduledFuture<?> newTimeoutTask = scheduler.schedule(() -> processAlbum(mediaGroupId, channelId), 
                                                                telegramProperties.getAlbumTimeoutSeconds(), TimeUnit.SECONDS);
        mediaGroupTimeouts.put(mediaGroupId, newTimeoutTask);
    }

    /**
    * Обработка завершенного альбома
    */
   private void processAlbum(String mediaGroupId, String channelId) {
       List<InputMedia> mediaList = mediaGroups.remove(mediaGroupId);
       Set<String> uniqueIds = mediaGroupUniqueIds.remove(mediaGroupId);
       mediaGroupTimeouts.remove(mediaGroupId);

       if (mediaList == null || mediaList.isEmpty()) {
           log.warn("No media found for group ID: {}", mediaGroupId);
           return;
       }

       try {
           // Отправляем альбом в закрытую группу
           List<Message> privateMessages = messageSender.sendAlbumToPrivateGroup(telegramProperties.getPrivateGroup().getId(), mediaList);
           Integer tempMessageId = privateMessages.get(0).getMessageId(); // messageId первого сообщения
           Long firstPrivateMessageId = Long.valueOf(tempMessageId); // Преобразуем в Long

           // Создаем ссылку
           String discussionLink = messageSender.createMessageLink(telegramProperties.getPrivateGroup().getId(), firstPrivateMessageId);

           // Обновляем ссылку в альбоме для канала
           if (!mediaList.isEmpty()) {
               mediaList.get(0).setCaption(discussionLink);
               mediaList.get(0).setParseMode("HTML");
           }

           // Отправляем альбом в канал
           List<Message> channelMessages = messageSender.sendAlbumToChannel(channelId, mediaGroupId, mediaList, discussionLink);

           // Сохраняем первый messageId из альбома
           Integer channelMessageId = channelMessages.get(0).getMessageId();
           Post post = new Post(channelMessageId, Long.valueOf(channelId), 
                                applicationProperties.getName(), firstPrivateMessageId);
           postRepository.save(post);

           log.info("Album with {} photos sent successfully, channelMessageId: {}", mediaList.size(), channelMessageId);
       } catch (TelegramApiException e) {
           log.error("Error processing album: {}", e.getMessage(), e);
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