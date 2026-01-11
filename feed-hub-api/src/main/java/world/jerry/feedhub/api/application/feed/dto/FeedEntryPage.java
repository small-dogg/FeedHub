package world.jerry.feedhub.api.application.feed.dto;

import java.util.List;

public record FeedEntryPage(
        List<FeedEntryInfo> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
    public static FeedEntryPage of(List<FeedEntryInfo> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = page < totalPages - 1;
        boolean hasPrevious = page > 0;
        return new FeedEntryPage(content, page, size, totalElements, totalPages, hasNext, hasPrevious);
    }
}
