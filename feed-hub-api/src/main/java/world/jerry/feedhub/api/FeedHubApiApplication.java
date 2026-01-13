package world.jerry.feedhub.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FeedHubApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeedHubApiApplication.class, args);
    }
}
