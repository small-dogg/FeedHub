package world.jerry.feedhub.collector.crawler;

import world.jerry.feedhub.collector.domain.CrawlResult;
import world.jerry.feedhub.collector.message.CrawlRequestMessage;
import world.jerry.feedhub.common.domain.BlogType;

/**
 * 블로그 크롤러 인터페이스
 * 각 블로그 플랫폼별로 구현체 제공
 */
public interface BlogCrawler {

    /**
     * 지원하는 블로그 타입 반환
     */
    BlogType getSupportedType();

    /**
     * 크롤링 수행
     */
    CrawlResult crawl(CrawlRequestMessage request);
}
