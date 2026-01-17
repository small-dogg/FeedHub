package world.jerry.feedhub.api.interfaces.rest.rss.dto;

import world.jerry.feedhub.api.application.rss.dto.RssInfoDetail;

import java.time.Instant;

public record RssSourceResponse(
        Long id,
        String blogName,
        String author,
        String rssUrl,
        String siteUrl,
        String language,
        Instant createdAt,
        Instant lastSyncAt
) {
    public static RssSourceResponse from(RssInfoDetail detail) {
        return new RssSourceResponse(
                detail.id(),
                detail.blogName(),
                detail.author(),
                detail.rssUrl(),
                detail.siteUrl(),
                detail.language(),
                detail.createdAt(),
                detail.lastSyncAt()
        );
    }
}
