package world.jerry.feedhub.collector.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import world.jerry.feedhub.collector.domain.RssInfo;
import world.jerry.feedhub.collector.repository.RssInfoRepository;
import world.jerry.feedhub.common.command.SyncAllFeedsCommand;
import world.jerry.feedhub.common.command.SyncRssFeedCommand;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Consumer for SyncAllFeedsCommand from Scheduler
 * Fan-Out Pattern: Receives SyncAll command and dispatches individual
 * SyncRssFeedCommand for each feed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncAllCommandConsumer {

        private final RssInfoRepository rssInfoRepository;
        private final CommandProducer commandProducer;

        @Transactional
        @KafkaListener(topics = "feedhub.commands.sync-all", groupId = "feed-hub-collector-group", containerFactory = "syncAllCommandListenerContainerFactory")
        public void consumeSyncAllCommand(ConsumerRecord<String, SyncAllFeedsCommand> record, Acknowledgment ack) {
                SyncAllFeedsCommand command = record.value();

                log.info("Received SyncAllFeedsCommand: commandId={}, requestedBy={}",
                                command.commandId(), command.requestedBy());

                List<RssInfo> allRssInfos = rssInfoRepository.findAll();
                log.info("Found {} RSS sources to sync. Starting Fan-Out...", allRssInfos.size());

                int dispatchCount = 0;
                int failCount = 0;

                for (RssInfo rssInfo : allRssInfos) {
                        try {
                                String syncCommandId = UUID.randomUUID().toString();

                                SyncRssFeedCommand syncCommand = new SyncRssFeedCommand(
                                                syncCommandId,
                                                rssInfo.getId(),
                                                rssInfo.getRssUrl(),
                                                rssInfo.getBlogName(),
                                                Instant.now(),
                                                command.requestedBy());

                                commandProducer.publish(syncCommand);
                                dispatchCount++;

                        } catch (Exception e) {
                                failCount++;
                                log.error("Failed to dispatch SyncRssFeedCommand: rssInfoId={}, blogName={}, error={}",
                                                rssInfo.getId(), rssInfo.getBlogName(), e.getMessage());
                        }
                }

                log.info("SyncAllFeedsCommand processing completed: commandId={}, dispatched={}, failedToDispatch={}",
                                command.commandId(), dispatchCount, failCount);

                ack.acknowledge();
        }
}
