package pr.vexor.telegrambot.babaykakeeper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BabaykaKeeperApplication {
    public static void main(String[] args) {
        SpringApplication.run(BabaykaKeeperApplication.class, args);
    }
}