package world.jerry.feedhub.common.event;

import world.jerry.feedhub.common.domain.BlogType;
import world.jerry.feedhub.common.domain.CrawlMode;

import java.time.Instant;
import java.util.List;

/**
 * 블로그 크롤링 완료 Event
 */
public record CrawlCompletedEvent(
        String eventId,
        String correlationId,
        Long rssInfoId,
        String blogName,
        BlogType blogType,
        CrawlMode crawlMode,
        int newEntriesCount,
        int skippedCount,
        List<Long> savedEntryIds,
        Instant occurredAt
) implements Event {
}
