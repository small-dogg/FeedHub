package world.jerry.feedhub.api.interfaces.rest.feed.dto;

import world.jerry.feedhub.api.application.feed.dto.FeedEntryInfo;

import java.time.Instant;
import java.util.List;

public record FeedEntryResponse(
        Long id,
        RssSourceSummary rssSource,
        String title,
        String link,
        String description,
        String author,
        Instant publishedAt,
        Long viewCount,
        List<TagSummary> tags
) {
    public static FeedEntryResponse from(FeedEntryInfo info) {
        List<TagSummary> tagSummaries = info.tags() != null
                ? info.tags().stream()
                        .map(t -> new TagSummary(t.id(), t.name()))
                        .toList()
                : List.of();
        return new FeedEntryResponse(
                info.id(),
                new RssSourceSummary(info.rssInfoId(), info.rssInfoBlogName(), info.rssInfoSiteUrl()),
                info.title(),
                info.link(),
                info.description(),
                info.author(),
                info.publishedAt(),
                info.viewCount(),
                tagSummaries
        );
    }

    public record RssSourceSummary(Long id, String blogName, String siteUrl) {}
    public record TagSummary(Long id, String name) {}
}
