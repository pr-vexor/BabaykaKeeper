package pr.vexor.telegrambot.babaykakeeper.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@Data
@ConfigurationProperties(prefix = "app.instance")
public class ApplicationProperties {

    private String name;
    private String activityFlagFilename;
    
}