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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 티스토리 블로그 크롤러
 * - 아티클 목록 페이지를 HTML 파싱
 * - ?page=N 쿼리로 페이지네이션
 */
@Slf4j
@Component
public class TistoryCrawler extends AbstractBlogCrawler {

    // 티스토리 날짜 형식: "2026. 1. 19." 또는 "2026-01-19" 또는 "2026/01/19"
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})[.\\s/-]+(\\d{1,2})[.\\s/-]+(\\d{1,2})");

    public TistoryCrawler(
            @Value("${crawler.max-pages-per-crawl:200}") int maxPagesPerCrawl,
            @Value("${crawler.request-delay-ms:1000}") long requestDelayMs) {
        super(maxPagesPerCrawl, requestDelayMs);
    }

    @Override
    public BlogType getSupportedType() {
        return BlogType.TISTORY;
    }

    @Override
    protected String buildPageUrl(CrawlRequestMessage request, int page) {
        String baseUrl = request.siteUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            // RSS URL에서 도메인 추출
            baseUrl = extractDomain(request.rssUrl());
        }
        // 끝에 슬래시 제거
        baseUrl = baseUrl.replaceAll("/+$", "");
        // 티스토리 전체 글 보기: /category?page=N
        return baseUrl + "/category?page=" + page;
    }

    @Override
    protected List<CrawledArticle> parseArticles(Document doc, CrawlRequestMessage request) {
        List<CrawledArticle> articles = new ArrayList<>();
        String domain = extractDomain(request.siteUrl() != null ? request.siteUrl() : request.rssUrl());

        // 티스토리 카테고리 페이지의 글 목록 셀렉터 (스킨별로 다름)
        Elements articleElements = doc.select(".post-item, .article-list li, .list_content > li, " +
                "article, div.item, .entry-item");

        log.debug("Found {} article elements with primary selector", articleElements.size());

        if (articleElements.isEmpty()) {
            // 대체 셀렉터: 포스트 번호 링크를 포함한 부모 요소
            Elements links = doc.select("a[href~=^/\\d+$]");
            if (!links.isEmpty()) {
                for (Element link : links) {
                    Element parent = link.parent();
                    if (parent != null && !articleElements.contains(parent)) {
                        articleElements.add(parent);
                    }
                }
            }
            log.debug("Found {} article elements with fallback selector", articleElements.size());
        }

        for (Element article : articleElements) {
            try {
                CrawledArticle crawledArticle = parseArticle(article, domain, request.blogName());
                if (crawledArticle != null && crawledArticle.link() != null && !crawledArticle.title().isBlank()) {
                    // 중복 방지
                    boolean isDuplicate = articles.stream()
                            .anyMatch(a -> a.link().equals(crawledArticle.link()));
                    if (!isDuplicate) {
                        articles.add(crawledArticle);
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to parse article element: {}", e.getMessage());
            }
        }

        log.info("Parsed {} articles from page: {}", articles.size(), domain);
        return articles;
    }

    private CrawledArticle parseArticle(Element article, String domain, String blogName) {
        // 포스트 링크 추출 - 티스토리는 /숫자 형태의 링크 사용
        Element linkEl = article.selectFirst("a[href~=^/\\d+$]");
        if (linkEl == null) {
            linkEl = article.selectFirst("a[href*='/entry/'], a.link_post, a.link_article");
        }
        if (linkEl == null) {
            // article 자체가 링크일 수 있음
            if (article.tagName().equals("a") && article.attr("href").matches("^/\\d+$")) {
                linkEl = article;
            }
        }
        if (linkEl == null) {
            return null;
        }

        String href = linkEl.attr("href");
        String link;
        if (href.startsWith("http")) {
            link = href;
        } else {
            link = domain + href;
        }

        // 카테고리/태그 페이지 제외
        if (link.contains("/category") || link.contains("/tag") || link.contains("/guestbook")) {
            return null;
        }

        // 제목 추출 - .title 또는 span.title 우선
        String title = "";
        Element titleEl = article.selectFirst("span.title, .title, .tit_post, h2, h3");
        if (titleEl != null) {
            title = titleEl.text();
        }
        if (title.isBlank() && linkEl != null) {
            // 링크 내부에서 제목 찾기
            Element innerTitle = linkEl.selectFirst("span.title, .title");
            if (innerTitle != null) {
                title = innerTitle.text();
            } else {
                title = linkEl.text().trim();
            }
        }
        if (title.isBlank()) {
            return null;
        }

        // 요약 추출
        String description = "";
        Element summaryEl = article.selectFirst("span.excerpt, .excerpt, .summary, .txt_post, p.desc");
        if (summaryEl != null) {
            description = summaryEl.text();
        }

        // 날짜 추출
        Instant publishedAt = parseDate(article);

        return CrawledArticle.builder()
                .title(title.trim())
                .link(link)
                .description(description.length() > 500 ? description.substring(0, 500) : description)
                .author(blogName)
                .publishedAt(publishedAt)
                .build();
    }

    private Instant parseDate(Element article) {
        // 날짜 관련 요소 찾기
        Element dateEl = article.selectFirst(".date, .post-date, time, [class*=date]");
        String dateText = dateEl != null ? dateEl.text() : article.text();

        Matcher matcher = DATE_PATTERN.matcher(dateText);
        if (matcher.find()) {
            try {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));
                LocalDate date = LocalDate.of(year, month, day);
                return date.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
            } catch (Exception e) {
                log.debug("Failed to parse date: {}", dateText);
            }
        }

        return Instant.now();
    }

    private String extractDomain(String url) {
        if (url == null) return "";
        return url.replaceAll("^(https?://[^/]+).*$", "$1");
    }
}
