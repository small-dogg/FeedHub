package world.jerry.feedhub.api.interfaces.rest.feed.dto;

import world.jerry.feedhub.api.application.feed.dto.FeedEntryInfo;

import java.time.Instant;

public record FeedEntryResponse(
        Long id,
        RssSourceSummary rssSource,
        String title,
        String link,
        String description,
        String author,
        Instant publishedAt
) {
    public static FeedEntryResponse from(FeedEntryInfo info) {
        return new FeedEntryResponse(
                info.id(),
                new RssSourceSummary(info.rssInfoId(), info.rssInfoBlogName()),
                info.title(),
                info.link(),
                info.description(),
                info.author(),
                info.publishedAt()
        );
    }

    public record RssSourceSummary(Long id, String blogName) {}
}
