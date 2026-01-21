package world.jerry.feedhub.common.event;

import java.time.Instant;
import java.util.List;

/**
 * RSS 피드 수집 완료 Event
 */
public record FeedEntriesCollectedEvent(
        String eventId,
        String correlationId,
        Long rssInfoId,
        String blogName,
        int newEntriesCount,
        int skippedCount,
        List<Long> savedEntryIds,
        Instant occurredAt
) implements Event {
}
