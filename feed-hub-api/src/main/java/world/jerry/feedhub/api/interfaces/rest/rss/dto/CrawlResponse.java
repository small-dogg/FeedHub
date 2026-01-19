package world.jerry.feedhub.api.interfaces.rest.rss.dto;

import world.jerry.feedhub.api.application.rss.CrawlService.CrawlRequestResult;

public record CrawlResponse(
        Long rssSourceId,
        String blogName,
        String blogType,
        boolean requested,
        String message
) {
    public static CrawlResponse from(CrawlRequestResult result) {
        return new CrawlResponse(
                result.rssInfoId(),
                result.blogName(),
                result.blogType().name(),
                result.requested(),
                result.message()
        );
    }
}
