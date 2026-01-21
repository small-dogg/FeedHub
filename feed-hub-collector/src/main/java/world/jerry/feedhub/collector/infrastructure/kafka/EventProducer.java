package world.jerry.feedhub.collector.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import world.jerry.feedhub.common.event.CrawlCompletedEvent;
import world.jerry.feedhub.common.event.Event;
import world.jerry.feedhub.common.event.FeedEntriesCollectedEvent;
import world.jerry.feedhub.common.event.SyncFailedEvent;

/**
 * 이벤트를 Kafka로 발행하는 Producer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventProducer {

    private static final String FEED_COLLECTED_TOPIC = "feedhub.events.feed-collected";
    private static final String CRAWL_COMPLETED_TOPIC = "feedhub.events.crawl-completed";
    private static final String SYNC_FAILED_TOPIC = "feedhub.events.sync-failed";

    private final KafkaTemplate<String, Event> kafkaTemplate;

    public void publish(FeedEntriesCollectedEvent event) {
        log.info("Publishing FeedEntriesCollectedEvent: rssInfoId={}, newEntries={}",
                event.rssInfoId(), event.newEntriesCount());
        kafkaTemplate.send(FEED_COLLECTED_TOPIC, event.rssInfoId().toString(), event);
    }

    public void publish(CrawlCompletedEvent event) {
        log.info("Publishing CrawlCompletedEvent: rssInfoId={}, newEntries={}",
                event.rssInfoId(), event.newEntriesCount());
        kafkaTemplate.send(CRAWL_COMPLETED_TOPIC, event.rssInfoId().toString(), event);
    }

    public void publish(SyncFailedEvent event) {
        log.warn("Publishing SyncFailedEvent: rssInfoId={}, error={}",
                event.rssInfoId(), event.errorMessage());
        kafkaTemplate.send(SYNC_FAILED_TOPIC, event.rssInfoId().toString(), event);
    }
}
