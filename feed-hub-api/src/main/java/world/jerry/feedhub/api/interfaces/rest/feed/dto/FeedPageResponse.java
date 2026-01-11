package world.jerry.feedhub.api.interfaces.rest.feed.dto;

import world.jerry.feedhub.api.application.feed.dto.FeedEntryPage;

import java.util.List;

public record FeedPageResponse(
        List<FeedEntryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
    public static FeedPageResponse from(FeedEntryPage feedPage) {
        List<FeedEntryResponse> content = feedPage.content().stream()
                .map(FeedEntryResponse::from)
                .toList();
        return new FeedPageResponse(
                content,
                feedPage.page(),
                feedPage.size(),
                feedPage.totalElements(),
                feedPage.totalPages(),
                feedPage.hasNext(),
                feedPage.hasPrevious()
        );
    }
}
