package world.jerry.feedhub.api.application.feed.dto;

/**
 * 피드 정렬 타입
 */
public enum FeedSortType {
    PUBLISHED_AT,  // 게시날짜 순 (기본값)
    LIKE_COUNT,    // 좋아요 수 순
    LIKED_AT       // 좋아요한 시간 순 (likedOnly=true일 때만 유효)
}
