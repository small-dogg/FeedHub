package world.jerry.feedhub.api.infrastructure.rss;

/**
 * RSS/Atom 피드의 메타데이터
 */
public record FeedMetadata(
        String title,
        String author,
        String siteUrl,
        String language
) {
}
