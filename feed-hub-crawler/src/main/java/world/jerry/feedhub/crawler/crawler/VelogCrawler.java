package world.jerry.feedhub.crawler.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import world.jerry.feedhub.crawler.domain.CrawlResult;
import world.jerry.feedhub.crawler.domain.CrawledArticle;
import world.jerry.feedhub.crawler.message.BlogType;
import world.jerry.feedhub.crawler.message.CrawlMode;
import world.jerry.feedhub.crawler.message.CrawlRequestMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Velog 블로그 크롤러
 * - GraphQL API 사용 (cursor 기반 페이지네이션)
 */
@Slf4j
@Component
public class VelogCrawler extends AbstractBlogCrawler {

    private static final String VELOG_GRAPHQL_ENDPOINT = "https://v2.velog.io/graphql";
    private static final Pattern USERNAME_PATTERN = Pattern.compile("velog\\.io/@([\\w-]+)");
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * GraphQL 페이지 결과 (아티클 목록 + 다음 페이지 cursor)
     */
    private record VelogPageResult(List<CrawledArticle> articles, String lastCursor) {}

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final int maxPagesPerCrawl;

    public VelogCrawler(
            @Value("${crawler.max-pages-per-crawl:10}") int maxPagesPerCrawl,
            @Value("${crawler.request-delay-ms:1000}") long requestDelayMs) {
        super(maxPagesPerCrawl, requestDelayMs);
        this.maxPagesPerCrawl = maxPagesPerCrawl;
        this.httpClient = new OkHttpClient.Builder().build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public BlogType getSupportedType() {
        return BlogType.VELOG;
    }

    @Override
    public CrawlResult crawl(CrawlRequestMessage request) {
        log.info("Starting Velog crawl: rssInfoId={}", request.rssInfoId());

        String username = extractUsername(request.siteUrl(), request.rssUrl());
        if (username == null) {
            log.error("Failed to extract username from URL");
            return CrawlResult.failure(request.rssInfoId(), "Username not found in URL");
        }

        List<CrawledArticle> allArticles = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        String cursor = null;
        int pageCount = 0;
        int maxPages = request.crawlMode() == CrawlMode.FULL ? maxPagesPerCrawl : 1;

        try {
            for (int page = 0; page < maxPages; page++) {
                VelogPageResult result = fetchPostsViaGraphQL(username, cursor, request.blogName());
                List<CrawledArticle> articles = result.articles();

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
                pageCount++;

                log.info("Page {} crawled: {} new articles (total: {})", pageCount, newArticles.size(), allArticles.size());

                // 다음 페이지를 위한 cursor 업데이트
                cursor = result.lastCursor();

                // 20개 미만이면 마지막 페이지
                if (articles.size() < 20) {
                    break;
                }
            }

            log.info("Velog crawl completed: rssInfoId={}, articles={}",
                    request.rssInfoId(), allArticles.size());

            return CrawlResult.success(request.rssInfoId(), allArticles, pageCount);

        } catch (Exception e) {
            log.error("Velog crawl failed: {}", e.getMessage(), e);
            return CrawlResult.failure(request.rssInfoId(), e.getMessage());
        }
    }

    private VelogPageResult fetchPostsViaGraphQL(String username, String cursor, String blogName) throws Exception {
        String query = buildGraphQLQuery(username, cursor);

        RequestBody body = RequestBody.create(query, JSON);
        Request request = new Request.Builder()
                .url(VELOG_GRAPHQL_ENDPOINT)
                .post(body)
                .header("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("GraphQL request failed: " + response.code());
            }

            String responseBody = response.body().string();
            return parseGraphQLResponse(responseBody, username, blogName);
        }
    }

    private String buildGraphQLQuery(String username, String cursor) {
        String cursorParam = cursor != null ? "\"" + cursor + "\"" : "null";
        return String.format("""
                {
                    "query": "query Posts($username: String!, $cursor: ID) { posts(username: $username, cursor: $cursor) { id title short_description url_slug released_at } }",
                    "variables": {
                        "username": "%s",
                        "cursor": %s
                    }
                }
                """, username, cursorParam);
    }

    private VelogPageResult parseGraphQLResponse(String responseBody, String username, String blogName) throws Exception {
        List<CrawledArticle> articles = new ArrayList<>();
        String lastCursor = null;

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode posts = root.path("data").path("posts");

        if (posts.isMissingNode() || !posts.isArray()) {
            return new VelogPageResult(articles, null);
        }

        for (JsonNode post : posts) {
            String id = post.path("id").asText("");
            String title = post.path("title").asText("");
            String urlSlug = post.path("url_slug").asText("");
            String description = post.path("short_description").asText("");
            String releasedAt = post.path("released_at").asText("");

            if (title.isBlank() || urlSlug.isBlank()) {
                continue;
            }

            // 마지막 포스트의 ID를 cursor로 사용
            lastCursor = id;

            String link = "https://velog.io/@" + username + "/" + urlSlug;

            Instant publishedAt;
            try {
                publishedAt = Instant.parse(releasedAt);
            } catch (Exception e) {
                publishedAt = Instant.now();
            }

            articles.add(CrawledArticle.builder()
                    .title(title)
                    .link(link)
                    .description(description.length() > 500 ? description.substring(0, 500) : description)
                    .author(blogName)
                    .publishedAt(publishedAt)
                    .build());
        }

        return new VelogPageResult(articles, lastCursor);
    }

    private String extractUsername(String siteUrl, String rssUrl) {
        String url = siteUrl != null && !siteUrl.isBlank() ? siteUrl : rssUrl;
        if (url == null) return null;

        Matcher matcher = USERNAME_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    @Override
    protected String buildPageUrl(CrawlRequestMessage request, int page) {
        // GraphQL 사용하므로 사용하지 않음
        return request.siteUrl();
    }

    @Override
    protected List<CrawledArticle> parseArticles(Document doc, CrawlRequestMessage request) {
        // GraphQL 사용하므로 사용하지 않음
        return List.of();
    }
}
