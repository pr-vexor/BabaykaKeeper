package pr.vexor.telegrambot.babaykakeeper.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.File;

@Slf4j
@Service
public class BotActivityManager {
        
    @Value("${app.instance.id}")
    private String instanceId;
    
    private volatile boolean isActive = false;

    private static final String ACTIVITY_FLAG_FILENAME = "babayka_bot_active.flag";
    
    @PostConstruct
    public void init() {
        checkActivityStatus();
        logStatus();
    }
    
    /**
     * Раз в 5 минут
     */
    @Scheduled(fixedRate = 5*60*1000)
    public void checkActivityStatus() {
        File activeFlag = new File(ACTIVITY_FLAG_FILENAME);
        this.isActive = activeFlag.exists();
    }
    
    /**
     * Создаёт локально временный файл - как флаг активности бота
     * @return true в случае успеха
     */
    public boolean activate() {
        try {
            File activeFlag = new File(ACTIVITY_FLAG_FILENAME);
            if (activeFlag.createNewFile()) {
                isActive = true;
                
                logStatus();
                return true;
            }
        } catch (Exception e) {
            log.error("Bot activation error: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Удаляет локальный временный файл-флаг активности бота
     * @return true в случае успеха
     */
    public boolean deactivate() {
        File activeFlag = new File(ACTIVITY_FLAG_FILENAME);
        if (activeFlag.delete()) {
            isActive = false;
            
            logStatus();
            return true;
        }
        return false;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public String getInstanceId() {
        return instanceId;
    }
    
    private void logStatus() {
        log.info("Bot instance `{}` is {}", instanceId, isActive ? "ACTIVE" : "STANDBY");
    }
}