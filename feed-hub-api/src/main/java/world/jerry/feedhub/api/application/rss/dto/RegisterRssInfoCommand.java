package world.jerry.feedhub.api.application.rss.dto;

public record RegisterRssInfoCommand(
        String blogName,
        String author,
        String rssUrl,
        String siteUrl,
        String language
) {
}
