package world.jerry.feedhub.api.interfaces.rest.feed.dto;

public record LikeResponse(
        Long feedEntryId,
        boolean liked,
        long likeCount
) {
}
