package pr.vexor.telegrambot.babaykakeeper.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
public class Post {
    
    @Id
    private Integer messageId;
    
    @Column(nullable = false)
    private Long channelChatId;
    
    private String processedByInstance;
    
    @Column(nullable = false)
    private LocalDateTime processedAt;
    
    @Column(nullable = false)
    private boolean hasButtons = false;
    
    private Integer privateGroupMessageId;
    
    public Post(Integer messageId, Long channelChatId, String instanceId, Integer friendsGroupMessageId) {
        this.messageId = messageId;
        this.channelChatId = channelChatId;
        this.processedByInstance = instanceId;
        this.processedAt = LocalDateTime.now();
        this.hasButtons = true;
        this.privateGroupMessageId = friendsGroupMessageId;
    }
    
}