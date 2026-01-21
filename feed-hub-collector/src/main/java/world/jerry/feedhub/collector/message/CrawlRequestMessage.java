package world.jerry.feedhub.collector.message;

import world.jerry.feedhub.common.domain.BlogType;
import world.jerry.feedhub.common.domain.CrawlMode;

import java.time.Instant;

/**
 * Kafka에서 수신하는 크롤링 요청 메시지
 * (향후 CrawlBlogCommand로 대체 예정)
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
