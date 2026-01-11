package world.jerry.feedhub.api.application.feed.dto;

import world.jerry.feedhub.api.domain.feed.FeedEntry;

import java.time.Instant;

public record FeedEntryInfo(
        Long id,
        Long rssInfoId,
        String rssInfoBlogName,
        String title,
        String link,
        String description,
        String author,
        Instant publishedAt
) {
    public static FeedEntryInfo from(FeedEntry entry, String blogName) {
        return new FeedEntryInfo(
                entry.getId(),
                entry.getRssInfoId(),
                blogName,
                entry.getTitle(),
                entry.getLink(),
                entry.getDescription(),
                entry.getAuthor(),
                entry.getPublishedAt()
        );
    }
}
