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

    protected static final int DEFAULT_TIMEOUT_MS = 15000;
    protected static final int MAX_RETRIES = 3;
    protected static final long RETRY_DELAY_MS = 2000;

    // 실제 브라우저와 유사한 User-Agent
    protected static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

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
     * - 브라우저와 유사한 헤더 설정으로 봇 차단 우회
     * - 재시도 로직 포함
     */
    protected Document fetchDocument(String url) throws IOException {
        IOException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Document doc = Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                        .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                        .header("Accept-Encoding", "gzip, deflate, br")
                        .header("Cache-Control", "no-cache")
                        .header("Pragma", "no-cache")
                        .header("Sec-Ch-Ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
                        .header("Sec-Ch-Ua-Mobile", "?0")
                        .header("Sec-Ch-Ua-Platform", "\"macOS\"")
                        .header("Sec-Fetch-Dest", "document")
                        .header("Sec-Fetch-Mode", "navigate")
                        .header("Sec-Fetch-Site", "none")
                        .header("Sec-Fetch-User", "?1")
                        .header("Upgrade-Insecure-Requests", "1")
                        .referrer("https://www.google.com/")
                        .timeout(DEFAULT_TIMEOUT_MS)
                        .followRedirects(true)
                        .ignoreHttpErrors(false)
                        .get();

                log.debug("Successfully fetched document from: {}", url);
                return doc;

            } catch (org.jsoup.HttpStatusException e) {
                lastException = e;
                int statusCode = e.getStatusCode();

                if (statusCode == 403 || statusCode == 429) {
                    log.warn("HTTP {} on attempt {}/{} for URL: {}", statusCode, attempt, MAX_RETRIES, url);

                    if (attempt < MAX_RETRIES) {
                        long delay = RETRY_DELAY_MS * attempt; // Exponential backoff
                        log.debug("Retrying after {}ms...", delay);
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Interrupted during retry", ie);
                        }
                    }
                } else {
                    // 다른 HTTP 에러는 즉시 실패
                    throw e;
                }
            } catch (IOException e) {
                lastException = e;
                log.warn("IO error on attempt {}/{} for URL: {} - {}", attempt, MAX_RETRIES, url, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted during retry", ie);
                    }
                }
            }
        }

        throw lastException != null ? lastException : new IOException("Failed to fetch document after " + MAX_RETRIES + " attempts");
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
