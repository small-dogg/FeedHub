package world.jerry.feedhub.api.application.feed.dto;

import java.time.Instant;
import java.util.List;

public record FeedEntryPage(
        List<FeedEntryInfo> content,
        Long lastId,
        Instant lastPublishedAt,
        boolean hasMore
) {
    public static FeedEntryPage of(List<FeedEntryInfo> content, boolean hasMore) {
        Long lastId = content.isEmpty() ? null : content.get(content.size() - 1).id();
        Instant lastPublishedAt = content.isEmpty() ? null : content.get(content.size() - 1).publishedAt();
        return new FeedEntryPage(content, lastId, lastPublishedAt, hasMore);
    }
}
