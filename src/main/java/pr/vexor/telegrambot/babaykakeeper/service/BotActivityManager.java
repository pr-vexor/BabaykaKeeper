package pr.vexor.telegrambot.babaykakeeper.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import pr.vexor.telegrambot.babaykakeeper.config.ApplicationProperties;

import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotActivityManager {
    
    private volatile boolean isActive = false;
    
    private final ApplicationProperties applicationProperties;

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
        File activeFlag = new File(applicationProperties.getActivityFlagFilename());
        isActive = activeFlag.exists();
    }
    
    /**
     * Создаёт локально временный файл - как флаг активности бота
     * @return true в случае успеха
     */
    public boolean activate() {
        try {
            File activeFlag = new File(applicationProperties.getActivityFlagFilename());
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
        File activeFlag = new File(applicationProperties.getActivityFlagFilename());
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
    
    public String getInstanceName() {
        return applicationProperties.getName();
    }
    
    private void logStatus() {
        log.info("Bot instance `{}` is {}", 
                applicationProperties.getName(), isActive ? "ACTIVE" : "STANDBY");
    }
}