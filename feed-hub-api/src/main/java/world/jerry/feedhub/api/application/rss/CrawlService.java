package world.jerry.feedhub.api.application.rss;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import world.jerry.feedhub.api.domain.rss.BlogType;
import world.jerry.feedhub.api.domain.rss.CrawlMode;
import world.jerry.feedhub.api.domain.rss.RssInfo;
import world.jerry.feedhub.api.domain.rss.RssInfoRepository;
import world.jerry.feedhub.api.infrastructure.kafka.BlogTypeDetector;
import world.jerry.feedhub.api.infrastructure.kafka.CrawlRequestProducer;

import java.util.NoSuchElementException;

/**
 * 블로그 크롤링 요청 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlService {

    private final RssInfoRepository rssInfoRepository;
    private final CrawlRequestProducer crawlRequestProducer;
    private final BlogTypeDetector blogTypeDetector;

    /**
     * 특정 RSS 소스에 대한 크롤링 요청
     * @return 크롤링 요청 결과 (블로그 타입, 요청 성공 여부)
     */
    public CrawlRequestResult requestCrawl(Long rssInfoId, CrawlMode crawlMode) {
        RssInfo rssInfo = rssInfoRepository.findById(rssInfoId)
                .orElseThrow(() -> new NoSuchElementException("RSS source not found: " + rssInfoId));

        BlogType blogType = blogTypeDetector.detect(rssInfo.getSiteUrl(), rssInfo.getRssUrl());

        if (blogType == BlogType.UNKNOWN) {
            log.info("Skipping crawl for UNKNOWN blog type: id={}, name={}",
                    rssInfoId, rssInfo.getBlogName());
            return new CrawlRequestResult(rssInfoId, rssInfo.getBlogName(), blogType, false,
                    "지원하지 않는 블로그 타입입니다.");
        }

        try {
            crawlRequestProducer.sendCrawlRequest(rssInfo, crawlMode);
            log.info("Crawl request sent: id={}, name={}, type={}",
                    rssInfoId, rssInfo.getBlogName(), blogType);
            return new CrawlRequestResult(rssInfoId, rssInfo.getBlogName(), blogType, true, null);
        } catch (Exception e) {
            log.error("Failed to send crawl request: id={}, error={}",
                    rssInfoId, e.getMessage());
            return new CrawlRequestResult(rssInfoId, rssInfo.getBlogName(), blogType, false,
                    "크롤링 요청 실패: " + e.getMessage());
        }
    }

    public record CrawlRequestResult(
            Long rssInfoId,
            String blogName,
            BlogType blogType,
            boolean requested,
            String message
    ) {}
}
