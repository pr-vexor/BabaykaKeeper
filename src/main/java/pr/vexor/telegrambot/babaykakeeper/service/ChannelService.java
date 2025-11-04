package pr.vexor.telegrambot.babaykakeeper.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

import pr.vexor.telegrambot.babaykakeeper.repository.ProcessedPostRepository;

@Slf4j
@Service
public class ChannelService {

    @Value("${telegram.channel-id}")
    private String channelId;

    @Value("${telegram.friends-group-id:NOT_SET_YET}")
    private String friendsGroupId;

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
            //Копируем пост в закрытую группу друзей
            Long friendsMessageId = messageSender.copyMessageToFriendsGroup(friendsGroupId, originalMessage);
            log.info("Post copied to friends group, friendsMessageId: {}", friendsMessageId);

            // Публикуем в канал с добавленной ссылкой
            String discussionLink = messageSender.createMessageLink(friendsGroupId, friendsMessageId);
            messageSender.sendPostToChannel(channelId, originalMessage, discussionLink);

            // todo: Сохраняем в БД — если нужно отслеживать
            // Пока пропускаем, так как не знаем ID в канале без доработки sendPostToChannel
            // Можно вернуть MessageId из sendPostToChannel и сохранить

            log.info("Post successfully published to channel via bot");

        } catch (Exception e) {
            log.error("Error publishing post via bot (tempId: {}): {}", tempId, e.getMessage(), e);
        }
    }
}