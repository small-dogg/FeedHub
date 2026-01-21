package world.jerry.feedhub.collector.crawler;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import world.jerry.feedhub.collector.domain.CrawlResult;
import world.jerry.feedhub.collector.domain.CrawledArticle;
import world.jerry.feedhub.common.domain.BlogType;
import world.jerry.feedhub.common.domain.CrawlMode;
import world.jerry.feedhub.collector.message.CrawlRequestMessage;

import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Medium 블로그 크롤러
 * - MediumUrlParser를 사용하여 다양한 URL 패턴 지원
 * - 아카이브 기반 페이지네이션
 * - 페이지 타입별 최적화된 셀렉터
 */
@Slf4j
@Component
public class MediumCrawler extends AbstractBlogCrawler {

    private final MediumUrlParser urlParser;
    private final int maxPagesPerCrawl;
    private final long requestDelayMs;

    // 현재 크롤링 컨텍스트 (페이지네이션용)
    private MediumUrlParser.ParseResult currentParseResult;
    private YearMonth currentArchiveMonth;

    public MediumCrawler(
            MediumUrlParser urlParser,
            @Value("${crawler.max-pages-per-crawl:10}") int maxPagesPerCrawl,
            @Value("${crawler.request-delay-ms:2000}") long requestDelayMs) {
        super(maxPagesPerCrawl, requestDelayMs);
        this.urlParser = urlParser;
        this.maxPagesPerCrawl = maxPagesPerCrawl;
        this.requestDelayMs = requestDelayMs;
    }

    @Override
    public BlogType getSupportedType() {
        return BlogType.MEDIUM;
    }

    @Override
    public CrawlResult crawl(CrawlRequestMessage request) {
        // Medium은 봇 차단으로 인해 크롤링 비활성화 - RSS 동기화 사용 권장
        log.info("Medium crawling is disabled. Use RSS sync instead. rssInfoId={}, siteUrl={}",
                request.rssInfoId(), request.siteUrl());

        return CrawlResult.success(request.rssInfoId(), List.of(), 0);

        // TODO: Medium 크롤링이 필요한 경우 아래 주석 해제
        /*
        log.info("Starting Medium crawl: rssInfoId={}, crawlUrl={}, siteUrl={}",
                request.rssInfoId(), request.crawlUrl(), request.siteUrl());

        // URL 분석
        currentParseResult = urlParser.parseWithFallback(
                request.crawlUrl(), request.siteUrl(), request.rssUrl());

        log.info("Detected Medium page type: {} for URL: {}",
                currentParseResult.getPageType(), currentParseResult.getCrawlUrl());

        // 아카이브 페이지네이션 초기화
        if (currentParseResult.getArchiveMonth() != null) {
            currentArchiveMonth = currentParseResult.getArchiveMonth();
        } else {
            currentArchiveMonth = YearMonth.now();
        }

        // Publication은 아카이브 페이지네이션 사용
        if (shouldUseArchivePagination(currentParseResult.getPageType())) {
            return crawlWithArchivePagination(request);
        }

        // 그 외는 기본 크롤링
        return super.crawl(request);
        */
    }

    /**
     * 아카이브 기반 페이지네이션으로 크롤링
     */
    private CrawlResult crawlWithArchivePagination(CrawlRequestMessage request) {
        List<CrawledArticle> allArticles = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        int pageCount = 0;
        int emptyPageCount = 0;
        int maxEmptyPages = 3; // 연속 빈 페이지 허용 횟수

        try {
            int maxPages = request.crawlMode() == CrawlMode.FULL ? maxPagesPerCrawl : 1;
            YearMonth crawlMonth = currentArchiveMonth;

            for (int page = 1; page <= maxPages && emptyPageCount < maxEmptyPages; page++) {
                String archiveUrl = urlParser.buildArchiveUrl(currentParseResult.getCrawlUrl(), crawlMonth);
                log.debug("Crawling archive page {}: {} (month: {})", page, archiveUrl, crawlMonth);

                try {
                    Document doc = fetchDocument(archiveUrl);
                    List<CrawledArticle> articles = parseArticles(doc, request);

                    if (articles.isEmpty()) {
                        emptyPageCount++;
                        log.debug("No articles found for month: {}, empty count: {}", crawlMonth, emptyPageCount);
                    } else {
                        emptyPageCount = 0; // 리셋

                        List<CrawledArticle> newArticles = articles.stream()
                                .filter(a -> !seenUrls.contains(normalizeUrl(a.link())))
                                .toList();

                        newArticles.forEach(a -> seenUrls.add(normalizeUrl(a.link())));
                        allArticles.addAll(newArticles);
                        pageCount++;

                        log.info("Archive {} crawled: {} new articles (total: {})",
                                crawlMonth, newArticles.size(), allArticles.size());
                    }

                    // 이전 달로 이동
                    crawlMonth = crawlMonth.minusMonths(1);

                    // Rate limiting
                    if (page < maxPages) {
                        Thread.sleep(requestDelayMs);
                    }

                } catch (Exception e) {
                    log.warn("Failed to crawl archive {}: {}", archiveUrl, e.getMessage());
                    emptyPageCount++;
                    crawlMonth = crawlMonth.minusMonths(1);
                }
            }

            log.info("Medium archive crawl completed: rssInfoId={}, months={}, articles={}",
                    request.rssInfoId(), pageCount, allArticles.size());

            return CrawlResult.success(request.rssInfoId(), allArticles, pageCount);

        } catch (Exception e) {
            log.error("Medium crawl failed: rssInfoId={}, error={}",
                    request.rssInfoId(), e.getMessage(), e);
            return CrawlResult.failure(request.rssInfoId(), e.getMessage());
        }
    }

    private boolean shouldUseArchivePagination(MediumUrlParser.PageType pageType) {
        return pageType == MediumUrlParser.PageType.PUBLICATION_ALL
                || pageType == MediumUrlParser.PageType.PUBLICATION_ARCHIVE
                || pageType == MediumUrlParser.PageType.CUSTOM_DOMAIN;
    }

    @Override
    protected String buildPageUrl(CrawlRequestMessage request, int page) {
        // 기본 크롤링 시 사용 (USER_PROFILE, TAGGED 등)
        return currentParseResult != null ? currentParseResult.getCrawlUrl() : request.siteUrl();
    }

    @Override
    protected List<CrawledArticle> parseArticles(Document doc, CrawlRequestMessage request) {
        List<CrawledArticle> articles = new ArrayList<>();

        // 페이지 타입에 따른 셀렉터 선택
        Elements articleElements = selectArticleElements(doc);

        if (articleElements.isEmpty()) {
            log.debug("No articles found with primary selectors, trying fallback");
            articleElements = doc.select("article, [data-post-id], div[class*='postArticle']");
        }

        for (Element article : articleElements) {
            try {
                CrawledArticle crawledArticle = parseArticle(article, request);
                if (crawledArticle != null && crawledArticle.link() != null && !crawledArticle.link().isBlank()) {
                    articles.add(crawledArticle);
                }
            } catch (Exception e) {
                log.debug("Failed to parse Medium article: {}", e.getMessage());
            }
        }

        log.debug("Parsed {} articles from Medium page", articles.size());
        return articles;
    }

    /**
     * 페이지 타입별 아티클 요소 선택
     */
    private Elements selectArticleElements(Document doc) {
        MediumUrlParser.PageType pageType = currentParseResult != null
                ? currentParseResult.getPageType()
                : MediumUrlParser.PageType.UNKNOWN;

        return switch (pageType) {
            case PUBLICATION_ALL, PUBLICATION_ARCHIVE, CUSTOM_DOMAIN ->
                // Publication 페이지 - 스트림 컨테이너 내 아티클
                    doc.select("div.streamItem, div[class*='streamItem'], article[data-post-id], " +
                            "div.js-postListHandle article, div[class*='postArticle']");

            case USER_PROFILE ->
                // 유저 프로필 - 포스트 목록
                    doc.select("article, div[class*='post'], div[data-post-id]");

            case TAGGED ->
                // 태그 페이지
                    doc.select("div.streamItem article, article[data-post-id], div[class*='post']");

            default ->
                // 기본 셀렉터
                    doc.select("article, [data-post-id], div[class*='postArticle'], div[class*='post']");
        };
    }

    private CrawledArticle parseArticle(Element article, CrawlRequestMessage request) {
        // 링크 추출 - 다양한 패턴 시도
        String link = extractLink(article);
        if (link == null || link.isBlank()) {
            return null;
        }

        // 제목 추출
        String title = extractTitle(article);
        if (title == null || title.isBlank()) {
            return null;
        }

        // 요약 추출
        String description = extractDescription(article);

        // 작성자 추출
        String author = extractAuthor(article, request.blogName());

        // 발행일 추출
        Instant publishedAt = extractPublishedAt(article);

        return CrawledArticle.builder()
                .title(title.trim())
                .link(link)
                .description(description != null && description.length() > 500
                        ? description.substring(0, 500)
                        : description)
                .author(author)
                .publishedAt(publishedAt)
                .build();
    }

    private String extractLink(Element article) {
        // 메인 아티클 링크 찾기
        Element linkEl = article.selectFirst(
                "a[data-action='open-post'], " +
                "h2 a, h3 a, " +
                "a[href*='/p/'], " +
                "a[href*='medium.com'][href*='?source=']"
        );

        if (linkEl == null) {
            linkEl = article.selectFirst("a[href]");
        }

        if (linkEl != null) {
            String href = linkEl.absUrl("href");
            // 쿼리 파라미터 제거하여 정규화
            if (href.contains("?")) {
                href = href.substring(0, href.indexOf("?"));
            }
            return href;
        }

        return null;
    }

    private String extractTitle(Element article) {
        // 제목 요소 찾기
        Element titleEl = article.selectFirst("h2, h3, h1, [class*='title']");
        if (titleEl != null) {
            return titleEl.text();
        }

        // 링크 텍스트를 제목으로 사용
        Element linkEl = article.selectFirst("a[data-action='open-post'], h2 a, h3 a");
        if (linkEl != null) {
            return linkEl.text();
        }

        return null;
    }

    private String extractDescription(Element article) {
        Element descEl = article.selectFirst(
                "p:not(:has(a)), " +
                "h4, " +
                "[class*='preview'], " +
                "[class*='subtitle'], " +
                ".graf--p"
        );

        return descEl != null ? descEl.text() : "";
    }

    private String extractAuthor(Element article, String defaultAuthor) {
        Element authorEl = article.selectFirst(
                "[class*='author'] a, " +
                "a[href*='/@'], " +
                "[data-author-id]"
        );

        if (authorEl != null) {
            String authorText = authorEl.text();
            if (authorText != null && !authorText.isBlank()) {
                return authorText;
            }
        }

        return defaultAuthor;
    }

    private Instant extractPublishedAt(Element article) {
        Element timeEl = article.selectFirst("time[datetime], [datetime]");

        if (timeEl != null && timeEl.hasAttr("datetime")) {
            try {
                return Instant.parse(timeEl.attr("datetime"));
            } catch (Exception e) {
                // 파싱 실패 시 현재 시간
            }
        }

        // 상대 시간 텍스트에서 추출 시도 (예: "3 days ago")
        Element relativeTime = article.selectFirst("[class*='time'], span:contains(ago)");
        if (relativeTime != null) {
            // 상대 시간 파싱은 복잡하므로 현재 시간 반환
            return Instant.now();
        }

        return Instant.now();
    }
}
