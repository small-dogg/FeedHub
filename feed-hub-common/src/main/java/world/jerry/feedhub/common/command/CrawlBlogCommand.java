package world.jerry.feedhub.common.command;

import world.jerry.feedhub.common.domain.BlogType;
import world.jerry.feedhub.common.domain.CrawlMode;

import java.time.Instant;

/**
 * 블로그 크롤링 요청 Command
 */
public record CrawlBlogCommand(
        String commandId,
        Long rssInfoId,
        String blogName,
        BlogType blogType,
        String siteUrl,
        String crawlUrl,
        String rssUrl,
        CrawlMode crawlMode,
        Instant requestedAt,
        String requestedBy
) implements Command {
}
