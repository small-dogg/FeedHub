package world.jerry.feedhub.api.application.feed.dto;

import java.util.List;

public record FeedSearchCriteria(
        List<Long> rssInfoIds,
        List<Long> tagIds,
        Long lastId,
        int size
) {
    public FeedSearchCriteria {
        if (size <= 0) size = 20;
        if (size > 100) size = 100;
    }
}
