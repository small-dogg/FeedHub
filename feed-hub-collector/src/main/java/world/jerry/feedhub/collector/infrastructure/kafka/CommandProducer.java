package world.jerry.feedhub.collector.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import world.jerry.feedhub.common.command.Command;
import world.jerry.feedhub.common.command.SyncRssFeedCommand;

/**
 * Command를 Kafka로 발행하는 Producer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandProducer {

    private static final String SYNC_RSS_TOPIC = "feedhub.commands.sync-rss";

    private final KafkaTemplate<String, Command> kafkaTemplate;

    public void publish(SyncRssFeedCommand command) {
        log.info("Publishing SyncRssFeedCommand: commandId={}, rssInfoId={}, blogName={}",
                command.commandId(), command.rssInfoId(), command.blogName());
        kafkaTemplate.send(SYNC_RSS_TOPIC, command.commandId(), command);
    }
}
