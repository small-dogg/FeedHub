package world.jerry.feedhub.api.application.feed.dto;

import java.util.List;

public record FeedSearchCriteria(
        List<Long> rssInfoIds,
        List<Long> tagIds,
        int page,
        int size
) {
    public FeedSearchCriteria {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 100) size = 100;
    }

    public long offset() {
        return (long) page * size;
    }
}
