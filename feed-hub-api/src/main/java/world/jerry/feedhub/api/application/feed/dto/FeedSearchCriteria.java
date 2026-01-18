package world.jerry.feedhub.api.application.feed.dto;

import java.time.Instant;
import java.util.List;

public record FeedSearchCriteria(
        List<Long> rssInfoIds,
        List<Long> tagIds,
        String query,
        Long lastId,
        Instant lastPublishedAt,
        int size
) {
    public FeedSearchCriteria {
        if (size <= 0) size = 20;
        if (size > 100) size = 100;
    }
}
