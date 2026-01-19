package world.jerry.feedhub.crawler.crawler;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import world.jerry.feedhub.crawler.domain.CrawlResult;
import world.jerry.feedhub.crawler.domain.CrawledArticle;
import world.jerry.feedhub.crawler.message.CrawlMode;
import world.jerry.feedhub.crawler.message.CrawlRequestMessage;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 공통 크롤링 로직 제공
 */
@Slf4j
public abstract class AbstractBlogCrawler implements BlogCrawler {

    protected static final int DEFAULT_TIMEOUT_MS = 10000;
    protected static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    private final int maxPagesPerCrawl;

    protected AbstractBlogCrawler(int maxPagesPerCrawl, long requestDelayMs) {
        this.maxPagesPerCrawl = maxPagesPerCrawl;
    }

    @Override
    public CrawlResult crawl(CrawlRequestMessage request) {
        log.info("Starting crawl: rssInfoId={}, blogType={}, mode={}",
                request.rssInfoId(), getSupportedType(), request.crawlMode());

        List<CrawledArticle> allArticles = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        int pageCount = 0;

        try {
            int maxPages = request.crawlMode() == CrawlMode.FULL ? maxPagesPerCrawl : 1;

            for (int page = 1; page <= maxPages; page++) {
                String pageUrl = buildPageUrl(request, page);
                log.debug("Crawling page {}: {}", page, pageUrl);

                Document doc = fetchDocument(pageUrl);
                List<CrawledArticle> articles = parseArticles(doc, request);

                if (articles.isEmpty()) {
                    log.debug("No more articles found at page {}", page);
                    break;
                }

                // 새로운 아티클만 필터링 (이미 수집한 URL 제외, 정규화된 URL로 비교)
                List<CrawledArticle> newArticles = articles.stream()
                        .filter(article -> !seenUrls.contains(normalizeUrl(article.link())))
                        .toList();

                // 모든 아티클이 중복이면 마지막 페이지로 판단하고 종료
                if (newArticles.isEmpty()) {
                    log.debug("All articles on page {} are duplicates, stopping crawl", page);
                    break;
                }

                // 수집한 URL 기록 (정규화된 URL로)
                newArticles.forEach(article -> seenUrls.add(normalizeUrl(article.link())));
                allArticles.addAll(newArticles);
                pageCount = page;

                log.info("Page {} crawled: {} new articles (total: {})", page, newArticles.size(), allArticles.size());
            }

            log.info("Crawl completed: rssInfoId={}, pages={}, articles={}",
                    request.rssInfoId(), pageCount, allArticles.size());

            return CrawlResult.success(request.rssInfoId(), allArticles, pageCount);

        } catch (Exception e) {
            log.error("Crawl failed: rssInfoId={}, error={}",
                    request.rssInfoId(), e.getMessage(), e);
            return CrawlResult.failure(request.rssInfoId(), e.getMessage());
        }
    }

    /**
     * 페이지 URL 생성 (각 크롤러에서 구현)
     */
    protected abstract String buildPageUrl(CrawlRequestMessage request, int page);

    /**
     * HTML Document에서 아티클 목록 파싱 (각 크롤러에서 구현)
     */
    protected abstract List<CrawledArticle> parseArticles(Document doc, CrawlRequestMessage request);

    /**
     * Jsoup을 사용하여 HTML Document 가져오기
     */
    protected Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(DEFAULT_TIMEOUT_MS)
                .get();
    }

    /**
     * URL 정규화 (디코딩하여 일관된 형식 유지)
     */
    protected String normalizeUrl(String url) {
        if (url == null) return null;
        try {
            return URLDecoder.decode(url, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return url;
        }
    }
}
