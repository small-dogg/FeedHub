package world.jerry.feedhub.collector.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import world.jerry.feedhub.collector.application.RssSyncHandler;
import world.jerry.feedhub.collector.domain.SyncHistory;
import world.jerry.feedhub.collector.repository.SyncHistoryRepository;
import world.jerry.feedhub.common.command.SyncRssFeedCommand;
import world.jerry.feedhub.common.event.FeedEntriesCollectedEvent;
import world.jerry.feedhub.common.event.SyncFailedEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * RSS 동기화 Command Consumer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncCommandConsumer {

    private final RssSyncHandler rssSyncHandler;
    private final EventProducer eventProducer;
    private final SyncHistoryRepository syncHistoryRepository;

    @Transactional
    @KafkaListener(
            topics = "feedhub.commands.sync-rss",
            groupId = "feed-hub-collector-group",
            containerFactory = "syncCommandListenerContainerFactory"
    )
    public void consumeSyncCommand(ConsumerRecord<String, SyncRssFeedCommand> record, Acknowledgment ack) {
        SyncRssFeedCommand command = record.value();

        log.info("Received SyncRssFeedCommand: commandId={}, rssInfoId={}, blogName={}",
                command.commandId(), command.rssInfoId(), command.blogName());

        // 동기화 이력 시작
        SyncHistory history = SyncHistory.started(
                command.commandId(),
                command.rssInfoId(),
                "RSS_SYNC",
                command.requestedBy()
        );
        syncHistoryRepository.save(history);

        try {
            FeedEntriesCollectedEvent event = rssSyncHandler.handle(command);
            eventProducer.publish(event);

            // 동기화 이력 완료
            history.complete(event.newEntriesCount(), event.skippedCount());
            syncHistoryRepository.save(history);

            log.info("SyncRssFeedCommand processed successfully: commandId={}, newEntries={}",
                    command.commandId(), event.newEntriesCount());

        } catch (Exception e) {
            log.error("Failed to process SyncRssFeedCommand: commandId={}, error={}",
                    command.commandId(), e.getMessage(), e);

            // 동기화 이력 실패
            history.fail(e.getMessage());
            syncHistoryRepository.save(history);

            SyncFailedEvent failedEvent = new SyncFailedEvent(
                    UUID.randomUUID().toString(),
                    command.commandId(),
                    command.rssInfoId(),
                    command.blogName(),
                    e.getMessage(),
                    e.getClass().getSimpleName(),
                    Instant.now()
            );
            eventProducer.publish(failedEvent);
        } finally {
            ack.acknowledge();
        }
    }
}
