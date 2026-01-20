package world.jerry.feedhub.crawler.message;

import java.time.Instant;

/**
 * Kafka에서 수신하는 크롤링 요청 메시지
 */
public record CrawlRequestMessage(
        Long rssInfoId,
        String blogName,
        BlogType blogType,
        String siteUrl,
        String crawlUrl,
        String rssUrl,
        CrawlMode crawlMode,
        Instant requestedAt
) {
}
