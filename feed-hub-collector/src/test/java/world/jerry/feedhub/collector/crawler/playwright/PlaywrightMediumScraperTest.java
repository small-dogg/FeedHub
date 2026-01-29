package world.jerry.feedhub.collector.crawler.playwright;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import world.jerry.feedhub.collector.domain.CrawledArticle;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Playwright Medium scraper pre-flight tests
 * - Validates bot detection bypass capability
 * - Tests article parsing accuracy
 * - Measures performance metrics
 *
 * NOTE: These tests require actual Medium URLs and network access
 */
@SpringBootTest
class PlaywrightMediumScraperTest {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightMediumScraperTest.class);

    @Autowired
    private PlaywrightMediumScraper scraper;

    // Test URLs - Replace with actual Medium article URLs for testing
    private static final String TEST_ARTICLE_URL = "https://medium.com/@username/sample-article-123";

    @BeforeEach
    void setUp() {
        assertNotNull(scraper, "PlaywrightMediumScraper should be autowired");
    }

    /**
     * Phase 1: Basic browser initialization test
     */
    @Test
    void testScraperInitialization() {
        log.info("Testing scraper initialization");
        assertNotNull(scraper, "Scraper should be initialized");
    }

    /**
     * Phase 2: Single article scraping test
     * - Tests bot detection bypass
     * - Validates article parsing
     *
     * IMPORTANT: Replace TEST_ARTICLE_URL with a real Medium article URL
     */
    @Test
    @Disabled("Requires valid Medium article URL - enable when ready to test with real URL")
    void testScrapeSingleArticle() {
        log.info("Testing single article scrape: {}", TEST_ARTICLE_URL);

        long startTime = System.currentTimeMillis();

        CrawledArticle article = scraper.scrape(TEST_ARTICLE_URL);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Scraping completed in {}ms", duration);

        // Validate article data
        assertNotNull(article, "Article should not be null");
        assertNotNull(article.title(), "Title should be extracted");
        assertFalse(article.title().isEmpty(), "Title should not be empty");
        assertNotEquals("Untitled", article.title(), "Title should not be default value");

        assertNotNull(article.description(), "Description should be extracted");
        assertFalse(article.description().isBlank(), "Description should not be blank");
        assertTrue(article.description().length() > 50,
                "Description should contain meaningful content");

        assertEquals(TEST_ARTICLE_URL, article.link(), "Link should match input URL");

        // Optional fields (may be null)
        log.info("Article data:");
        log.info("  Title: {}", article.title());
        log.info("  Author: {}", article.author());
        log.info("  Published: {}", article.publishedAt());
        log.info("  Description length: {}", article.description().length());

        // Performance validation
        assertTrue(duration < 30000,
                "Scraping should complete within 30 seconds (actual: " + duration + "ms)");
    }

    /**
     * Phase 3: Bot detection bypass validation
     * - Tests if Medium blocks the request
     * - Critical for determining if Playwright approach is viable
     *
     * IMPORTANT: This is the most critical test
     */
    @Test
    @Disabled("Requires valid Medium article URL - enable when ready to test bot detection")
    void testBotDetectionBypass() {
        log.info("Testing bot detection bypass");

        try {
            CrawledArticle article = scraper.scrape(TEST_ARTICLE_URL);

            // If we reach here without exception, bot detection is bypassed
            assertNotNull(article, "Article should be successfully scraped");
            log.info("SUCCESS: Bot detection bypassed!");
            log.info("  Title: {}", article.title());

        } catch (RuntimeException e) {
            String message = e.getMessage();
            log.error("FAILED: Bot detection not bypassed - {}", message);

            // Check if it's a 403 (Forbidden) error
            if (message.contains("403")) {
                fail("Bot detection active: HTTP 403 Forbidden");
            } else if (message.contains("429")) {
                fail("Rate limiting active: HTTP 429 Too Many Requests");
            } else {
                fail("Scraping failed: " + message);
            }
        }
    }

    /**
     * Phase 4: Multiple articles scraping test
     * - Tests rate limiting behavior
     * - Validates continuous scraping capability
     */
    @Test
    @Disabled("Requires multiple valid Medium article URLs")
    void testMultipleArticlesScraping() {
        List<String> testUrls = Arrays.asList(
                "https://medium.com/@user/article-1",
                "https://medium.com/@user/article-2",
                "https://medium.com/@user/article-3"
        );

        log.info("Testing multiple articles scraping: {} URLs", testUrls.size());

        long startTime = System.currentTimeMillis();

        List<CrawledArticle> articles = scraper.scrapeMultiple(testUrls);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Multiple scraping completed in {}ms", duration);

        // Validate results
        assertNotNull(articles, "Articles list should not be null");
        assertEquals(testUrls.size(), articles.size(),
                "Should successfully scrape all articles");

        // Validate each article
        for (int i = 0; i < articles.size(); i++) {
            CrawledArticle article = articles.get(i);
            log.info("Article {}: {}", i + 1, article.title());

            assertNotNull(article.title(), "Article " + (i + 1) + " should have title");
            assertFalse(article.description().isBlank(),
                    "Article " + (i + 1) + " should have description");
        }

        // Performance validation (should include rate limiting delays)
        long expectedMinDuration = testUrls.size() * 3000; // 3 seconds per article
        assertTrue(duration >= expectedMinDuration,
                "Should respect rate limiting (expected >= " + expectedMinDuration + "ms, actual: " + duration + "ms)");
    }

    /**
     * Phase 5: Parsing accuracy test
     * - Validates all fields are extracted correctly
     */
    @Test
    @Disabled("Requires article URL with known content for validation")
    void testParsingAccuracy() {
        // Use an article where you know the exact content
        String knownArticleUrl = "https://medium.com/@user/known-article";
        String expectedTitle = "Expected Article Title";
        String expectedAuthor = "Expected Author Name";

        log.info("Testing parsing accuracy");

        CrawledArticle article = scraper.scrape(knownArticleUrl);

        assertNotNull(article, "Article should not be null");
        assertEquals(expectedTitle, article.title(), "Title should match expected value");
        assertEquals(expectedAuthor, article.author(), "Author should match expected value");
        assertNotNull(article.publishedAt(), "Published date should be extracted");

        log.info("Parsing accuracy validated successfully");
    }
}
