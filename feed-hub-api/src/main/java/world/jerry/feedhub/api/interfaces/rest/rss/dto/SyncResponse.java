package world.jerry.feedhub.api.interfaces.rest.rss.dto;

import world.jerry.feedhub.api.application.rss.dto.SyncResult;

import java.time.Instant;

public record SyncResponse(
        Long rssSourceId,
        String blogName,
        int syncedCount,
        int skippedCount,
        Instant lastSyncAt
) {
    public static SyncResponse from(SyncResult result) {
        return new SyncResponse(
                result.rssInfoId(),
                result.blogName(),
                result.syncedCount(),
                result.skippedCount(),
                result.lastSyncAt()
        );
    }
}
