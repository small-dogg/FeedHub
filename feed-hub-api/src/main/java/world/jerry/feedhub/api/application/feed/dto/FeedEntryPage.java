package world.jerry.feedhub.api.application.feed.dto;

import java.time.Instant;
import java.util.List;

/**
 * 피드 페이지 응답 DTO
 */
public record FeedEntryPage(
        List<FeedEntryInfo> content,
        Long lastId,
        Instant lastPublishedAt,
        Long lastLikeCount,
        Instant lastLikedAt,
        boolean hasMore
) {
    /**
     * PUBLISHED_AT 정렬용
     */
    public static FeedEntryPage of(List<FeedEntryInfo> content, boolean hasMore) {
        Long lastId = content.isEmpty() ? null : content.get(content.size() - 1).id();
        Instant lastPublishedAt = content.isEmpty() ? null : content.get(content.size() - 1).publishedAt();
        return new FeedEntryPage(content, lastId, lastPublishedAt, null, null, hasMore);
    }

    /**
     * sortType에 따른 커서 필드 설정
     */
    public static FeedEntryPage of(List<FeedEntryInfo> content, boolean hasMore, FeedSortType sortType) {
        if (content.isEmpty()) {
            return new FeedEntryPage(content, null, null, null, null, hasMore);
        }

        FeedEntryInfo lastEntry = content.get(content.size() - 1);
        Long lastId = lastEntry.id();
        Instant lastPublishedAt = lastEntry.publishedAt();
        Long lastLikeCount = null;
        Instant lastLikedAt = null;

        switch (sortType) {
            case LIKE_COUNT -> lastLikeCount = lastEntry.likeCount();
            case LIKED_AT -> lastLikedAt = lastEntry.likedAt();
        }

        return new FeedEntryPage(content, lastId, lastPublishedAt, lastLikeCount, lastLikedAt, hasMore);
    }
}
