package world.jerry.feedhub.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FeedHubSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeedHubSchedulerApplication.class, args);
    }
}
