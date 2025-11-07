package pr.vexor.telegrambot.babaykakeeper.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

import pr.vexor.telegrambot.babaykakeeper.config.TelegramProperties;
import pr.vexor.telegrambot.babaykakeeper.model.Post;
import pr.vexor.telegrambot.babaykakeeper.repository.ProcessedPostRepository;

@Slf4j
@Service
public class ChannelService {

    @Value("${app.instance.name}")
    private String instanceName;

    private MessageSenderService messageSender;
    private ProcessedPostRepository postRepository;
    private TelegramProperties telegramProperties;

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

            // Публикуем в канал с добавленной ссылкой
            String discussionLink = messageSender.createMessageLink(privateGroupId, privateMessageId);
            Integer channelMessageId = messageSender.sendPostToChannel(channelId, originalMessage, discussionLink);

            Post post = new Post(channelMessageId, Long.valueOf(channelId), instanceName, privateMessageId);
            postRepository.save(post);

            log.info("Post successfully published to channel via bot and saved to DB");

        } catch (Exception e) {
            log.error("Error publishing post via bot (tempId: {}): {}", tempId, e.getMessage(), e);
        }
    }
}