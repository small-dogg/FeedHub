package world.jerry.feedhub.collector.crawler.playwright;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import world.jerry.feedhub.collector.domain.CrawledArticle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for PlaywrightMediumScraper
 * - Tests full article scraping flow
 * - Validates bot detection bypass
 * - Checks parsing accuracy
 */
@SpringBootTest
class PlaywrightMediumScraperIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightMediumScraperIntegrationTest.class);

    @Autowired
    private PlaywrightMediumScraper scraper;

    // Test with Medium's official blog (stable, publicly accessible)
    private static final String MEDIUM_BLOG_URL = "https://blog.medium.com";

    @Test
    void testScrapeRealMediumPage() {
        log.info("=== Testing Real Medium Page Scraping ===");
        log.info("URL: {}", MEDIUM_BLOG_URL);

        long startTime = System.currentTimeMillis();

        try {
            CrawledArticle article = scraper.scrape(MEDIUM_BLOG_URL);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Scraping completed in {}ms", duration);

            // Validate article data
            assertNotNull(article, "Article should not be null");
            log.info("✓ Article scraped successfully");

            // Validate fields
            assertNotNull(article.title(), "Title should be extracted");
            assertFalse(article.title().isEmpty(), "Title should not be empty");
            log.info("  Title: {}", article.title());

            assertNotNull(article.link(), "Link should be set");
            assertEquals(MEDIUM_BLOG_URL, article.link(), "Link should match input URL");

            assertNotNull(article.description(), "Description should be extracted");
            log.info("  Description length: {} chars", article.description().length());

            if (article.author() != null) {
                log.info("  Author: {}", article.author());
            } else {
                log.info("  Author: (not extracted)");
            }

            if (article.publishedAt() != null) {
                log.info("  Published: {}", article.publishedAt());
            } else {
                log.info("  Published: (not extracted)");
            }

            // Performance validation
            // Note: Medium pages can be slow to load due to heavy JavaScript (90-120s observed)
            assertTrue(duration < 120000,
                    "Scraping should complete within 120 seconds (actual: " + duration + "ms)");

            log.info("=== SUCCESS: Playwright Medium scraping is VIABLE ===");

        } catch (RuntimeException e) {
            log.error("!!! Scraping failed: {}", e.getMessage(), e);

            if (e.getMessage().contains("403")) {
                log.error("!!! CRITICAL: Bot detection active (HTTP 403) !!!");
                log.error("Recommendation: Abort Playwright integration, use RSS only");
                fail("Bot detection not bypassed: HTTP 403");
            } else if (e.getMessage().contains("429")) {
                log.error("!!! Rate limiting active (HTTP 429) !!!");
                fail("Too many requests: HTTP 429");
            } else {
                fail("Scraping failed: " + e.getMessage());
            }
        }
    }
}
