package world.jerry.feedhub.api.application.rss.dto;

import world.jerry.feedhub.common.domain.BlogType;

public record RegisterRssInfoCommand(
        String blogName,
        String author,
        String rssUrl,
        String siteUrl,
        String crawlUrl,
        String language,
        BlogType blogType  // 수동 지정 시 사용, null이면 자동 감지
) {
}
