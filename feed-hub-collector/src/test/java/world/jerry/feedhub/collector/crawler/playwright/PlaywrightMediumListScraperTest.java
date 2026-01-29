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
 * Test for scrapeArticleList - list page scraping strategy
 * - Single page load extracts multiple articles
 * - Validates author and date extraction
 */
@SpringBootTest
class PlaywrightMediumListScraperTest {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightMediumListScraperTest.class);

    @Autowired
    private PlaywrightMediumScraper scraper;

    private static final String MEDIUM_BLOG_URL = "https://blog.medium.com";

    @Test
    void testScrapeArticleList() {
        log.info("=== Testing Article List Scraping ===");
        log.info("URL: {}", MEDIUM_BLOG_URL);

        long startTime = System.currentTimeMillis();

        List<CrawledArticle> articles = scraper.scrapeArticleList(MEDIUM_BLOG_URL);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Scraping completed in {}ms", duration);

        // Should find multiple articles
        assertNotNull(articles, "Articles list should not be null");
        assertFalse(articles.isEmpty(), "Should find at least one article");
        log.info("Found {} articles", articles.size());

        // Validate each article
        int articlesWithAuthor = 0;
        int articlesWithDate = 0;

        for (int i = 0; i < articles.size(); i++) {
            CrawledArticle article = articles.get(i);
            log.info("\n--- Article {} ---", i + 1);

            // Title is required
            assertNotNull(article.title(), "Title should not be null");
            assertFalse(article.title().isEmpty(), "Title should not be empty");
            log.info("  Title: {}", truncate(article.title(), 60));

            // Link should be present
            if (article.link() != null) {
                log.info("  Link: {}", truncate(article.link(), 80));
            }

            // Summary (description) - optional but expected
            if (article.description() != null && !article.description().isEmpty()) {
                log.info("  Summary: {}", truncate(article.description(), 60));
            }

            // Author - track success rate
            if (article.author() != null && !article.author().isEmpty()) {
                log.info("  Author: {}", article.author());
                articlesWithAuthor++;
            } else {
                log.info("  Author: (not extracted)");
            }

            // Published date - track success rate
            if (article.publishedAt() != null) {
                log.info("  Date: {}", article.publishedAt());
                articlesWithDate++;
            } else {
                log.info("  Date: (not extracted)");
            }
        }

        // Report extraction success rates
        log.info("\n=== Extraction Success Rates ===");
        log.info("  Articles found: {}", articles.size());
        log.info("  With author: {}/{} ({}%)", articlesWithAuthor, articles.size(),
                articles.isEmpty() ? 0 : (articlesWithAuthor * 100 / articles.size()));
        log.info("  With date: {}/{} ({}%)", articlesWithDate, articles.size(),
                articles.isEmpty() ? 0 : (articlesWithDate * 100 / articles.size()));

        // Performance check - should be fast (single page load)
        assertTrue(duration < 30000,
                "List scraping should complete within 30 seconds (actual: " + duration + "ms)");

        // At least some articles should have author/date
        assertTrue(articlesWithAuthor > 0, "At least one article should have author extracted");
        assertTrue(articlesWithDate > 0, "At least one article should have date extracted");

        log.info("\n=== SUCCESS: List scraping works correctly ===");
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
