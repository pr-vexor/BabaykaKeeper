package pr.vexor.telegrambot.babaykakeeper.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

import pr.vexor.telegrambot.babaykakeeper.model.Post;
import pr.vexor.telegrambot.babaykakeeper.repository.ProcessedPostRepository;

@Slf4j
@Service
public class ChannelService {

    @Value("${telegram.channel-id}")
    private String channelId;

    @Value("${telegram.friends-group-id:NOT_SET_YET}")
    private String friendsGroupId;
    
    @Value("${app.instance.name}")
    private String instanceName;

    @Autowired
    private MessageSenderService messageSender;

    @Autowired
    private ProcessedPostRepository postRepository;

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

        try {
            // Копируем пост в закрытую группу друзей
            Long friendsMessageId = messageSender.copyMessageToFriendsGroup(friendsGroupId, originalMessage);
            log.info("Post copied to friends group, friendsMessageId: {}", friendsMessageId);

            // Публикуем в канал с добавленной ссылкой
            String discussionLink = messageSender.createMessageLink(friendsGroupId, friendsMessageId);
            Integer channelMessageId = messageSender.sendPostToChannel(channelId, originalMessage, discussionLink);

            Post post = new Post(channelMessageId, Long.valueOf(channelId), instanceName, friendsMessageId);
            postRepository.save(post);

            log.info("Post successfully published to channel via bot and saved to DB");

        } catch (Exception e) {
            log.error("Error publishing post via bot (tempId: {}): {}", tempId, e.getMessage(), e);
        }
    }
}