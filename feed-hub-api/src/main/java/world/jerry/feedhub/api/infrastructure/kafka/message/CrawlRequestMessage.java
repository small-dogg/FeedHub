package world.jerry.feedhub.api.infrastructure.kafka.message;

import world.jerry.feedhub.common.domain.BlogType;
import world.jerry.feedhub.common.domain.CrawlMode;

import java.time.Instant;

/**
 * Kafka로 전송되는 크롤링 요청 메시지
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
    public static CrawlRequestMessage of(
            Long rssInfoId,
            String blogName,
            BlogType blogType,
            String siteUrl,
            String crawlUrl,
            String rssUrl,
            CrawlMode crawlMode
    ) {
        return new CrawlRequestMessage(
                rssInfoId,
                blogName,
                blogType,
                siteUrl,
                crawlUrl,
                rssUrl,
                crawlMode,
                Instant.now()
        );
    }
}
