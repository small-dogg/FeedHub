package world.jerry.feedhub.api.application.rss.dto;

import java.util.List;

public record RegisterRssInfoCommand(
        String blogName,
        String author,
        String rssUrl,
        String siteUrl,
        String language,
        List<Long> tagIds
) {
}
