package world.jerry.feedhub.api.application.rss.dto;

import java.time.Instant;

public record SyncResult(
        Long rssInfoId,
        String blogName,
        int syncedCount,
        int skippedCount,
        Instant lastSyncAt
) {
}
