package world.jerry.feedhub.crawler.crawler;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import world.jerry.feedhub.crawler.domain.CrawledArticle;
import world.jerry.feedhub.crawler.message.BlogType;
import world.jerry.feedhub.crawler.message.CrawlRequestMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Medium 블로그 크롤러
 * - RSS 피드 우선 사용
 * - HTML 파싱 보조
 */
@Slf4j
@Component
public class MediumCrawler extends AbstractBlogCrawler {

    public MediumCrawler(
            @Value("${crawler.max-pages-per-crawl:10}") int maxPagesPerCrawl,
            @Value("${crawler.request-delay-ms:1000}") long requestDelayMs) {
        super(maxPagesPerCrawl, requestDelayMs);
    }

    @Override
    public BlogType getSupportedType() {
        return BlogType.MEDIUM;
    }

    @Override
    protected String buildPageUrl(CrawlRequestMessage request, int page) {
        // Medium은 RSS 기반이므로 RSS URL 사용
        // 페이지네이션은 지원하지 않음 (RSS에서 최신 포스트만 제공)
        String siteUrl = request.siteUrl();
        if (siteUrl == null || siteUrl.isBlank()) {
            siteUrl = extractSiteUrl(request.rssUrl());
        }
        return siteUrl;
    }

    @Override
    protected List<CrawledArticle> parseArticles(Document doc, CrawlRequestMessage request) {
        List<CrawledArticle> articles = new ArrayList<>();

        // Medium 아티클 셀렉터
        Elements articleElements = doc.select("article, [data-post-id], .postArticle");

        if (articleElements.isEmpty()) {
            // 대체 셀렉터
            articleElements = doc.select("div[class*=post], div[class*=story]");
        }

        for (Element article : articleElements) {
            try {
                CrawledArticle crawledArticle = parseArticle(article, request);
                if (crawledArticle != null && crawledArticle.link() != null) {
                    articles.add(crawledArticle);
                }
            } catch (Exception e) {
                log.debug("Failed to parse Medium article: {}", e.getMessage());
            }
        }

        return articles;
    }

    private CrawledArticle parseArticle(Element article, CrawlRequestMessage request) {
        // 제목과 링크 추출
        Element titleLink = article.selectFirst("a[href*='medium.com'], h2 a, h3 a, [data-action='open-post'] a");
        if (titleLink == null) {
            titleLink = article.selectFirst("a");
        }
        if (titleLink == null) {
            return null;
        }

        String link = titleLink.absUrl("href");
        if (link.isBlank()) {
            return null;
        }

        // 제목 추출
        String title = "";
        Element titleEl = article.selectFirst("h2, h3, h1");
        if (titleEl != null) {
            title = titleEl.text();
        } else {
            title = titleLink.text();
        }

        if (title.isBlank()) {
            return null;
        }

        // 요약 추출
        Element summaryEl = article.selectFirst("p, .graf--p, [class*=preview]");
        String description = summaryEl != null ? summaryEl.text() : "";

        // 날짜 추출 (Medium은 상대시간을 많이 사용)
        Element timeEl = article.selectFirst("time");
        Instant publishedAt = null;
        if (timeEl != null && timeEl.hasAttr("datetime")) {
            try {
                publishedAt = Instant.parse(timeEl.attr("datetime"));
            } catch (Exception e) {
                publishedAt = Instant.now();
            }
        } else {
            publishedAt = Instant.now();
        }

        return CrawledArticle.builder()
                .title(title.trim())
                .link(link)
                .description(description.length() > 500 ? description.substring(0, 500) : description)
                .author(request.blogName())
                .publishedAt(publishedAt)
                .build();
    }

    private String extractSiteUrl(String rssUrl) {
        if (rssUrl == null) return "";
        // medium.com/@username/feed -> medium.com/@username
        return rssUrl.replace("/feed", "").replace("/rss", "");
    }
}
