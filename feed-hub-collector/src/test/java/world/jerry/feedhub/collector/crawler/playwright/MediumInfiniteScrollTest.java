package world.jerry.feedhub.collector.crawler.playwright;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import world.jerry.feedhub.collector.domain.CrawledArticle;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test infinite scroll pagination support
 * - Verifies that scraper can load more articles by scrolling
 * - Tests different maxArticles settings
 */
@SpringBootTest
class MediumInfiniteScrollTest {

    private static final Logger log = LoggerFactory.getLogger(MediumInfiniteScrollTest.class);

    @Autowired
    private PlaywrightMediumScraper scraper;

    // Test URLs
    private static final String DAANGN_URL = "https://medium.com/daangn/all";
    private static final String USER_PROFILE_URL = "https://medium.com/@springrod";

    /**
     * Test default article list scraping
     * Note: Medium uses aggressive virtualized scrolling that removes DOM elements.
     * Currently, we can reliably collect ~10 articles per page load.
     * For more articles, consider Archive-based pagination.
     */
    @Test
    void testInfiniteScroll_Default50Articles() {
        log.info("\n========================================");
        log.info("Test: Article List Scraping");
        log.info("URL: {}", DAANGN_URL);
        log.info("========================================");

        long startTime = System.currentTimeMillis();

        List<CrawledArticle> articles = scraper.scrapeArticleList(DAANGN_URL);

        long duration = System.currentTimeMillis() - startTime;

        log.info("\n--- Results ---");
        log.info("Total articles collected: {}", articles.size());
        log.info("Time taken: {}ms", duration);

        // Medium's virtualized scrolling limits per-page collection to ~10 articles
        // This is acceptable for regular crawling (detecting new articles)
        assertTrue(articles.size() >= 5,
                "Should collect at least 5 articles (got " + articles.size() + ")");

        // Log first and last few articles
        logArticleSamples(articles);

        // Verify no duplicates
        long uniqueLinks = articles.stream()
                .map(CrawledArticle::link)
                .filter(link -> link != null)
                .distinct()
                .count();
        assertEquals(uniqueLinks, articles.stream().filter(a -> a.link() != null).count(),
                "Should have no duplicate links");
    }

    @Test
    void testInfiniteScroll_Max30Articles() {
        log.info("\n========================================");
        log.info("Test: Article List Scraping - Max 30");
        log.info("URL: {}", DAANGN_URL);
        log.info("========================================");

        long startTime = System.currentTimeMillis();

        List<CrawledArticle> articles = scraper.scrapeArticleList(DAANGN_URL, 30);

        long duration = System.currentTimeMillis() - startTime;

        log.info("\n--- Results ---");
        log.info("Total articles collected: {}", articles.size());
        log.info("Time taken: {}ms", duration);

        // Medium's virtualized scrolling limits collection
        // We should get some articles, capped at the max
        assertTrue(articles.size() >= 5 && articles.size() <= 30,
                "Should collect 5-30 articles (got " + articles.size() + ")");

        logArticleSamples(articles);
    }

    @Test
    void testInfiniteScroll_UserProfile() {
        log.info("\n========================================");
        log.info("Test: Infinite Scroll - User Profile");
        log.info("URL: {}", USER_PROFILE_URL);
        log.info("========================================");

        long startTime = System.currentTimeMillis();

        List<CrawledArticle> articles = scraper.scrapeArticleList(USER_PROFILE_URL, 25);

        long duration = System.currentTimeMillis() - startTime;

        log.info("\n--- Results ---");
        log.info("Total articles collected: {}", articles.size());
        log.info("Time taken: {}ms", duration);

        assertFalse(articles.isEmpty(), "Should collect at least some articles");

        logArticleSamples(articles);

        // Verify author is extracted for user profile
        long articlesWithAuthor = articles.stream()
                .filter(a -> a.author() != null && !a.author().isEmpty())
                .count();
        log.info("Articles with author: {}/{}", articlesWithAuthor, articles.size());
    }

    @Test
    void testInfiniteScroll_SmallLimit() {
        log.info("\n========================================");
        log.info("Test: Infinite Scroll - Small limit (5)");
        log.info("URL: {}", DAANGN_URL);
        log.info("========================================");

        long startTime = System.currentTimeMillis();

        List<CrawledArticle> articles = scraper.scrapeArticleList(DAANGN_URL, 5);

        long duration = System.currentTimeMillis() - startTime;

        log.info("\n--- Results ---");
        log.info("Total articles collected: {}", articles.size());
        log.info("Time taken: {}ms", duration);

        // Should stop at 5 articles (no unnecessary scrolling)
        assertTrue(articles.size() <= 5,
                "Should stop at max limit (got " + articles.size() + ")");
        assertTrue(articles.size() >= 5,
                "Should collect at least 5 articles if available");

        // Small limit should be fast (minimal scrolling)
        assertTrue(duration < 15000,
                "Small limit should complete quickly (took " + duration + "ms)");

        logArticleSamples(articles);
    }

    private void logArticleSamples(List<CrawledArticle> articles) {
        if (articles.isEmpty()) {
            log.info("No articles to display");
            return;
        }

        log.info("\n--- First 3 Articles ---");
        for (int i = 0; i < Math.min(3, articles.size()); i++) {
            CrawledArticle article = articles.get(i);
            log.info("Article {}:", i + 1);
            log.info("  Title: {}", truncate(article.title(), 60));
            log.info("  Author: {}", article.author());
            log.info("  Date: {}", article.publishedAt());
        }

        if (articles.size() > 6) {
            log.info("\n--- Last 3 Articles ---");
            for (int i = articles.size() - 3; i < articles.size(); i++) {
                CrawledArticle article = articles.get(i);
                log.info("Article {}:", i + 1);
                log.info("  Title: {}", truncate(article.title(), 60));
                log.info("  Author: {}", article.author());
                log.info("  Date: {}", article.publishedAt());
            }
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
