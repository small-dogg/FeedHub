package world.jerry.feedhub.common.event;

import java.time.Instant;

/**
 * 동기화/크롤링 실패 Event
 */
public record SyncFailedEvent(
        String eventId,
        String correlationId,
        Long rssInfoId,
        String blogName,
        String errorMessage,
        String errorType,
        Instant occurredAt
) implements Event {
}
