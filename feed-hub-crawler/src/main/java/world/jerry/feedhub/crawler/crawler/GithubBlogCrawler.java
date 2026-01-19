package world.jerry.feedhub.crawler.crawler;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import world.jerry.feedhub.crawler.domain.CrawlResult;
import world.jerry.feedhub.crawler.domain.CrawledArticle;
import world.jerry.feedhub.crawler.message.BlogType;
import world.jerry.feedhub.crawler.message.CrawlMode;
import world.jerry.feedhub.crawler.message.CrawlRequestMessage;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub Blog (*.github.io) 크롤러
 * - sitemap.xml 또는 RSS 피드 우선 사용
 * - HTML 파싱 보조
 */
@Slf4j
@Component
public class GithubBlogCrawler extends AbstractBlogCrawler {

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})");

    public GithubBlogCrawler(
            @Value("${crawler.max-pages-per-crawl:10}") int maxPagesPerCrawl,
            @Value("${crawler.request-delay-ms:1000}") long requestDelayMs) {
        super(maxPagesPerCrawl, requestDelayMs);
    }

    @Override
    public BlogType getSupportedType() {
        return BlogType.GITHUB_BLOG;
    }

    @Override
    public CrawlResult crawl(CrawlRequestMessage request) {
        log.info("Starting GitHub Blog crawl: rssInfoId={}", request.rssInfoId());

        String baseUrl = request.siteUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = extractDomain(request.rssUrl());
        }

        List<CrawledArticle> allArticles = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        try {
            // 1. sitemap.xml 시도
            List<CrawledArticle> sitemapArticles = crawlFromSitemap(baseUrl, request);
            if (!sitemapArticles.isEmpty()) {
                // 중복 제거 (정규화된 URL로 비교)
                for (CrawledArticle article : sitemapArticles) {
                    String normalizedLink = normalizeUrl(article.link());
                    if (!seenUrls.contains(normalizedLink)) {
                        seenUrls.add(normalizedLink);
                        allArticles.add(article);
                    }
                }
                log.info("Found {} articles from sitemap", allArticles.size());
            } else {
                // 2. 메인 페이지 크롤링
                List<CrawledArticle> htmlArticles = crawlFromHtml(baseUrl, request);
                // 중복 제거 (정규화된 URL로 비교)
                for (CrawledArticle article : htmlArticles) {
                    String normalizedLink = normalizeUrl(article.link());
                    if (!seenUrls.contains(normalizedLink)) {
                        seenUrls.add(normalizedLink);
                        allArticles.add(article);
                    }
                }
                log.info("Found {} articles from HTML", allArticles.size());
            }

            return CrawlResult.success(request.rssInfoId(), allArticles, 1);

        } catch (Exception e) {
            log.error("GitHub Blog crawl failed: {}", e.getMessage(), e);
            return CrawlResult.failure(request.rssInfoId(), e.getMessage());
        }
    }

    private List<CrawledArticle> crawlFromSitemap(String baseUrl, CrawlRequestMessage request) {
        List<CrawledArticle> articles = new ArrayList<>();

        try {
            String sitemapUrl = baseUrl.replaceAll("/+$", "") + "/sitemap.xml";
            Document doc = Jsoup.connect(sitemapUrl)
                    .userAgent(USER_AGENT)
                    .timeout(DEFAULT_TIMEOUT_MS)
                    .get();

            Elements urls = doc.select("url");
            for (Element urlElement : urls) {
                String loc = urlElement.selectFirst("loc").text();
                String lastmod = urlElement.selectFirst("lastmod") != null
                        ? urlElement.selectFirst("lastmod").text() : null;

                // 포스트 URL 필터링 (보통 /yyyy/mm/dd/ 또는 /posts/ 패턴)
                if (isPostUrl(loc)) {
                    Instant publishedAt = parseLastmod(lastmod);

                    articles.add(CrawledArticle.builder()
                            .title(extractTitleFromUrl(loc))
                            .link(loc)
                            .description("")
                            .author(request.blogName())
                            .publishedAt(publishedAt)
                            .build());
                }
            }
        } catch (Exception e) {
            log.debug("Sitemap not available: {}", e.getMessage());
        }

        return articles;
    }

    private List<CrawledArticle> crawlFromHtml(String baseUrl, CrawlRequestMessage request) {
        List<CrawledArticle> articles = new ArrayList<>();

        try {
            Document doc = fetchDocument(baseUrl);

            // Jekyll/Hugo 등 일반적인 블로그 테마 셀렉터
            Elements postElements = doc.select("article, .post, .post-item, .entry, [class*=post-link]");

            if (postElements.isEmpty()) {
                // 대체 셀렉터
                postElements = doc.select("ul.posts li, .posts li, main a[href*='/20']");
            }

            for (Element post : postElements) {
                try {
                    CrawledArticle article = parsePostElement(post, baseUrl, request);
                    if (article != null) {
                        articles.add(article);
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse post element: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("HTML crawl failed: {}", e.getMessage());
        }

        return articles;
    }

    private CrawledArticle parsePostElement(Element post, String baseUrl, CrawlRequestMessage request) {
        Element link = post.selectFirst("a[href]");
        if (link == null) {
            return null;
        }

        String href = link.attr("href");
        if (!href.startsWith("http")) {
            href = baseUrl.replaceAll("/+$", "") + (href.startsWith("/") ? href : "/" + href);
        }

        String title = link.text();
        if (title.isBlank()) {
            Element titleEl = post.selectFirst("h2, h3, .title, .post-title");
            title = titleEl != null ? titleEl.text() : extractTitleFromUrl(href);
        }

        if (title.isBlank()) {
            return null;
        }

        // 날짜 추출
        Element dateEl = post.selectFirst("time, .date, .post-date, [class*=date]");
        Instant publishedAt = null;
        if (dateEl != null) {
            if (dateEl.hasAttr("datetime")) {
                try {
                    publishedAt = Instant.parse(dateEl.attr("datetime"));
                } catch (Exception ignored) {}
            }
            if (publishedAt == null) {
                publishedAt = parseDateFromText(dateEl.text());
            }
        }
        if (publishedAt == null) {
            publishedAt = parseDateFromText(href);
        }
        if (publishedAt == null) {
            publishedAt = Instant.now();
        }

        // 요약 추출
        Element summaryEl = post.selectFirst("p, .excerpt, .summary");
        String description = summaryEl != null ? summaryEl.text() : "";

        return CrawledArticle.builder()
                .title(title.trim())
                .link(href)
                .description(description.length() > 500 ? description.substring(0, 500) : description)
                .author(request.blogName())
                .publishedAt(publishedAt)
                .build();
    }

    private boolean isPostUrl(String url) {
        // 포스트 URL 패턴 (날짜 포함 또는 /posts/, /blog/ 경로)
        return url.matches(".*/(\\d{4})/(\\d{1,2})/(\\d{1,2})/.*")
                || url.contains("/posts/")
                || url.contains("/blog/")
                || url.contains("/articles/");
    }

    private String extractTitleFromUrl(String url) {
        // URL에서 제목 추출 (마지막 세그먼트)
        String[] parts = url.replaceAll("/$", "").split("/");
        if (parts.length > 0) {
            String last = parts[parts.length - 1];
            return last.replace("-", " ").replace("_", " ").replaceAll("\\.html?$", "");
        }
        return "";
    }

    private Instant parseLastmod(String lastmod) {
        if (lastmod == null) return Instant.now();
        try {
            return Instant.parse(lastmod);
        } catch (Exception e) {
            return parseDateFromText(lastmod);
        }
    }

    private Instant parseDateFromText(String text) {
        if (text == null) return null;
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));
                return LocalDate.of(year, month, day)
                        .atStartOfDay(ZoneId.of("UTC"))
                        .toInstant();
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String extractDomain(String url) {
        if (url == null) return "";
        return url.replaceAll("^(https?://[^/]+).*$", "$1");
    }

    @Override
    protected String buildPageUrl(CrawlRequestMessage request, int page) {
        return request.siteUrl();
    }

    @Override
    protected List<CrawledArticle> parseArticles(Document doc, CrawlRequestMessage request) {
        return List.of();
    }
}
