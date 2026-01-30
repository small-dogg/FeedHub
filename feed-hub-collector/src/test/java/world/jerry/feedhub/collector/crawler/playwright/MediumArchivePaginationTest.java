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
 * Test Archive-based pagination for collecting more articles
 * - Uses monthly archive URLs: /publication/archive/YYYY/MM
 * - More reliable than infinite scroll
 */
@SpringBootTest
class MediumArchivePaginationTest {

    private static final Logger log = LoggerFactory.getLogger(MediumArchivePaginationTest.class);

    @Autowired
    private PlaywrightMediumScraper scraper;

    private static final String DAANGN_URL = "https://medium.com/daangn";

    @Test
    void testArchivePagination_3Months() {
        log.info("\n========================================");
        log.info("Test: Archive Pagination - 3 months");
        log.info("URL: {}", DAANGN_URL);
        log.info("========================================");

        long startTime = System.currentTimeMillis();

        // Collect articles from last 3 months
        List<CrawledArticle> articles = scraper.scrapeWithArchivePagination(DAANGN_URL, 3, 50);

        long duration = System.currentTimeMillis() - startTime;

        log.info("\n--- Results ---");
        log.info("Total articles collected: {}", articles.size());
        log.info("Time taken: {}ms", duration);

        // Should collect more than single page load
        assertFalse(articles.isEmpty(), "Should collect at least some articles");

        // Log samples
        logArticleSamples(articles);

        // Verify no duplicates
        long uniqueLinks = articles.stream()
                .map(CrawledArticle::link)
                .filter(link -> link != null)
                .distinct()
                .count();

        log.info("Unique links: {}/{}", uniqueLinks, articles.size());
        assertEquals(uniqueLinks, articles.stream().filter(a -> a.link() != null).count(),
                "Should have no duplicate links");
    }

    @Test
    void testArchivePagination_6Months_Max30() {
        log.info("\n========================================");
        log.info("Test: Archive Pagination - 6 months, max 30");
        log.info("URL: {}", DAANGN_URL);
        log.info("========================================");

        long startTime = System.currentTimeMillis();

        List<CrawledArticle> articles = scraper.scrapeWithArchivePagination(DAANGN_URL, 6, 30);

        long duration = System.currentTimeMillis() - startTime;

        log.info("\n--- Results ---");
        log.info("Total articles collected: {}", articles.size());
        log.info("Time taken: {}ms", duration);

        assertTrue(articles.size() <= 30, "Should respect max articles limit");
        assertFalse(articles.isEmpty(), "Should collect some articles");

        logArticleSamples(articles);
    }

    @Test
    void testCompare_ListVsArchive() {
        log.info("\n========================================");
        log.info("Test: Compare List scraping vs Archive pagination");
        log.info("URL: {}", DAANGN_URL);
        log.info("========================================");

        // Method 1: Simple list scraping (single page)
        long start1 = System.currentTimeMillis();
        List<CrawledArticle> listArticles = scraper.scrapeArticleList(DAANGN_URL + "/all");
        long duration1 = System.currentTimeMillis() - start1;

        log.info("\n--- List Scraping (single page) ---");
        log.info("Articles: {}", listArticles.size());
        log.info("Time: {}ms", duration1);

        // Method 2: Archive pagination (3 months)
        long start2 = System.currentTimeMillis();
        List<CrawledArticle> archiveArticles = scraper.scrapeWithArchivePagination(DAANGN_URL, 3, 50);
        long duration2 = System.currentTimeMillis() - start2;

        log.info("\n--- Archive Pagination (3 months) ---");
        log.info("Articles: {}", archiveArticles.size());
        log.info("Time: {}ms", duration2);

        log.info("\n--- Comparison ---");
        log.info("Archive collected {}x more articles",
                listArticles.isEmpty() ? "N/A" :
                String.format("%.1f", (double) archiveArticles.size() / listArticles.size()));

        // Archive should typically collect more articles
        assertTrue(archiveArticles.size() >= listArticles.size(),
                "Archive pagination should collect at least as many articles as list scraping");
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
                log.info("  Date: {}", article.publishedAt());
            }
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
