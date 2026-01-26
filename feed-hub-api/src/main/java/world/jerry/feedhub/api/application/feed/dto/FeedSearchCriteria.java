package world.jerry.feedhub.api.application.feed.dto;

import java.time.Instant;
import java.util.List;

/**
 * 피드 검색 조건
 */
public record FeedSearchCriteria(
        Long memberId,
        List<Long> rssInfoIds,
        List<Long> tagIds,
        String query,
        Long lastId,
        Instant lastPublishedAt,
        Long lastLikeCount,
        Instant lastLikedAt,
        FeedSortType sortType,
        Boolean likedOnly,
        Boolean followedOnly,
        int size) {
    public FeedSearchCriteria {
        if (size <= 0)
            size = 20;
        if (size > 100)
            size = 100;
        if (sortType == null)
            sortType = FeedSortType.PUBLISHED_AT;
        if (likedOnly == null)
            likedOnly = false;
        if (followedOnly == null)
            followedOnly = false;
    }
}
