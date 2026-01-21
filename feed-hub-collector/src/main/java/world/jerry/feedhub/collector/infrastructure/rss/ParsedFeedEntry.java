package world.jerry.feedhub.collector.infrastructure.rss;

import java.time.Instant;

public record ParsedFeedEntry(
        String title,
        String link,
        String description,
        String author,
        Instant publishedAt,
        String guid
) {
}
