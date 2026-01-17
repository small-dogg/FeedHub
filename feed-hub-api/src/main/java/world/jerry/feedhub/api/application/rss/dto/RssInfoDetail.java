package world.jerry.feedhub.api.application.rss.dto;

import world.jerry.feedhub.api.domain.rss.RssInfo;

import java.time.Instant;

public record RssInfoDetail(
        Long id,
        String blogName,
        String author,
        String rssUrl,
        String siteUrl,
        String language,
        Instant createdAt,
        Instant lastSyncAt
) {
    public static RssInfoDetail from(RssInfo rssInfo) {
        return new RssInfoDetail(
                rssInfo.getId(),
                rssInfo.getBlogName(),
                rssInfo.getAuthor(),
                rssInfo.getRssUrl(),
                rssInfo.getSiteUrl(),
                rssInfo.getLanguage(),
                rssInfo.getCreatedAt(),
                rssInfo.getLastSyncAt()
        );
    }
}
