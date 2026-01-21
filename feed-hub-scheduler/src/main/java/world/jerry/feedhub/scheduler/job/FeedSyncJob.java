package world.jerry.feedhub.scheduler.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import world.jerry.feedhub.common.command.Command;
import world.jerry.feedhub.common.command.SyncAllFeedsCommand;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Scheduled job for RSS feed synchronization
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.feed-sync.enabled", havingValue = "true", matchIfMissing = true)
public class FeedSyncJob {

    private static final String SYNC_ALL_TOPIC = "feedhub.commands.sync-all";
    private static final String REQUESTER = "scheduler";

    private final KafkaTemplate<String, Command> commandKafkaTemplate;

    /**
     * Trigger RSS sync for all feeds periodically
     */
    @Scheduled(cron = "${scheduler.feed-sync.cron:0 0 * * * *}")
    public void syncAllFeeds() {
        String commandId = UUID.randomUUID().toString();
        log.info("Starting scheduled feed sync: commandId={}", commandId);

        SyncAllFeedsCommand command = new SyncAllFeedsCommand(
                commandId,
                Instant.now(),
                REQUESTER
        );

        CompletableFuture<SendResult<String, Command>> future =
                commandKafkaTemplate.send(SYNC_ALL_TOPIC, commandId, command);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send SyncAllFeedsCommand: commandId={}, error={}",
                        commandId, ex.getMessage());
            } else {
                log.info("SyncAllFeedsCommand sent: commandId={}, topic={}, partition={}, offset={}",
                        commandId,
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
