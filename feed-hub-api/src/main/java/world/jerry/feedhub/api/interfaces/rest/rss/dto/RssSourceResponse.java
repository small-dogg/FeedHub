package world.jerry.feedhub.api.interfaces.rest.rss.dto;

import world.jerry.feedhub.api.application.rss.dto.RssInfoDetail;
import world.jerry.feedhub.api.interfaces.rest.tag.dto.TagResponse;

import java.time.Instant;
import java.util.List;

public record RssSourceResponse(
        Long id,
        String blogName,
        String author,
        String rssUrl,
        String siteUrl,
        String language,
        Instant createdAt,
        Instant lastSyncAt,
        List<TagResponse> tags
) {
    public static RssSourceResponse from(RssInfoDetail detail) {
        List<TagResponse> tagResponses = detail.tags().stream()
                .map(TagResponse::from)
                .toList();
        return new RssSourceResponse(
                detail.id(),
                detail.blogName(),
                detail.author(),
                detail.rssUrl(),
                detail.siteUrl(),
                detail.language(),
                detail.createdAt(),
                detail.lastSyncAt(),
                tagResponses
        );
    }
}
