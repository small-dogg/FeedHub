package world.jerry.feedhub.collector.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import world.jerry.feedhub.collector.application.RssSyncHandler;
import world.jerry.feedhub.collector.domain.RssInfo;
import world.jerry.feedhub.collector.domain.SyncHistory;
import world.jerry.feedhub.collector.repository.RssInfoRepository;
import world.jerry.feedhub.collector.repository.SyncHistoryRepository;
import world.jerry.feedhub.common.command.SyncAllFeedsCommand;
import world.jerry.feedhub.common.command.SyncRssFeedCommand;
import world.jerry.feedhub.common.event.FeedEntriesCollectedEvent;
import world.jerry.feedhub.common.event.SyncFailedEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Consumer for SyncAllFeedsCommand from Scheduler
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncAllCommandConsumer {

    private final RssInfoRepository rssInfoRepository;
    private final RssSyncHandler rssSyncHandler;
    private final EventProducer eventProducer;
    private final SyncHistoryRepository syncHistoryRepository;

    @Transactional
    @KafkaListener(
            topics = "feedhub.commands.sync-all",
            groupId = "feed-hub-collector-group",
            containerFactory = "syncAllCommandListenerContainerFactory"
    )
    public void consumeSyncAllCommand(ConsumerRecord<String, SyncAllFeedsCommand> record, Acknowledgment ack) {
        SyncAllFeedsCommand command = record.value();

        log.info("Received SyncAllFeedsCommand: commandId={}, requestedBy={}",
                command.commandId(), command.requestedBy());

        List<RssInfo> allRssInfos = rssInfoRepository.findAll();
        log.info("Found {} RSS sources to sync", allRssInfos.size());

        int successCount = 0;
        int failCount = 0;

        for (RssInfo rssInfo : allRssInfos) {
            String syncCommandId = UUID.randomUUID().toString();

            // 동기화 이력 시작
            SyncHistory history = SyncHistory.started(
                    syncCommandId,
                    rssInfo.getId(),
                    "RSS_SYNC",
                    command.requestedBy()
            );
            syncHistoryRepository.save(history);

            try {
                SyncRssFeedCommand syncCommand = new SyncRssFeedCommand(
                        syncCommandId,
                        rssInfo.getId(),
                        rssInfo.getRssUrl(),
                        rssInfo.getBlogName(),
                        Instant.now(),
                        command.requestedBy()
                );

                FeedEntriesCollectedEvent event = rssSyncHandler.handle(syncCommand);
                eventProducer.publish(event);

                // 동기화 이력 완료
                history.complete(event.newEntriesCount(), event.skippedCount());
                syncHistoryRepository.save(history);

                successCount++;

                log.debug("Synced RSS source: rssInfoId={}, blogName={}, newEntries={}",
                        rssInfo.getId(), rssInfo.getBlogName(), event.newEntriesCount());

            } catch (Exception e) {
                failCount++;
                log.error("Failed to sync RSS source: rssInfoId={}, blogName={}, error={}",
                        rssInfo.getId(), rssInfo.getBlogName(), e.getMessage());

                // 동기화 이력 실패
                history.fail(e.getMessage());
                syncHistoryRepository.save(history);

                SyncFailedEvent failedEvent = new SyncFailedEvent(
                        UUID.randomUUID().toString(),
                        command.commandId(),
                        rssInfo.getId(),
                        rssInfo.getBlogName(),
                        e.getMessage(),
                        e.getClass().getSimpleName(),
                        Instant.now()
                );
                eventProducer.publish(failedEvent);
            }
        }

        log.info("SyncAllFeedsCommand completed: commandId={}, success={}, failed={}",
                command.commandId(), successCount, failCount);

        ack.acknowledge();
    }
}
