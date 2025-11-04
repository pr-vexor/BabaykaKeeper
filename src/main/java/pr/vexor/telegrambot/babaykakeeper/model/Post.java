package pr.vexor.telegrambot.babaykakeeper.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class Post {
    
    @Id
    private Integer messageId;
    
    @Column(nullable = false)
    private Long channelChatId;
    
    private String processedBy;
    
    @Column(nullable = false)
    private LocalDateTime processedAt;
    
    @Column(nullable = false)
    private boolean hasButton = false;
    
    private Long privateGroupMessageId;
    
    public Post(Integer messageId, Long channelChatId, String instanceName, Long friendsGroupMessageId) {
        this.messageId = messageId;
        this.channelChatId = channelChatId;
        this.processedBy = instanceName;
        this.processedAt = LocalDateTime.now();
        this.hasButton = true;
        this.privateGroupMessageId = friendsGroupMessageId;
    }
}