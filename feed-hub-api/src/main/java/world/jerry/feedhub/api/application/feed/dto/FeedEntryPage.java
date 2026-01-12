package world.jerry.feedhub.api.application.feed.dto;

import java.util.List;

public record FeedEntryPage(
        List<FeedEntryInfo> content,
        Long lastId,
        boolean hasMore
) {
    public static FeedEntryPage of(List<FeedEntryInfo> content, boolean hasMore) {
        Long lastId = content.isEmpty() ? null : content.get(content.size() - 1).id();
        return new FeedEntryPage(content, lastId, hasMore);
    }
}
