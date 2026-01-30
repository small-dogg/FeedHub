package world.jerry.feedhub.collector.crawler.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Detailed analysis of Medium's scroll behavior to understand virtualized DOM
 */
@SpringBootTest
class MediumScrollAnalysisTest {

    private static final Logger log = LoggerFactory.getLogger(MediumScrollAnalysisTest.class);

    @Autowired
    private Browser browser;

    private static final String DAANGN_URL = "https://medium.com/daangn/all";

    @Test
    void analyzeScrollBehavior() {
        log.info("=== Analyzing Medium Scroll Behavior ===");

        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .setViewportSize(1920, 1080)
        );

        Page page = context.newPage();

        try {
            page.navigate(DAANGN_URL, new Page.NavigateOptions()
                    .setTimeout(30000)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            page.waitForTimeout(3000);

            // Initial state
            log.info("\n=== INITIAL STATE ===");
            logPageState(page);

            // Try scrolling to end of page to trigger infinite scroll
            log.info("\n=== SCROLL TEST: Check if page has more content to load ===");

            // Get page scroll metrics
            Object initialScrollHeight = page.evaluate("document.body.scrollHeight");
            Object initialClientHeight = page.evaluate("document.documentElement.clientHeight");
            log.info("Initial scrollHeight: {}, clientHeight: {}", initialScrollHeight, initialClientHeight);

            // Scroll to last article and check if new ones load
            log.info("\n=== Scrolling to last article ===");
            page.evaluate("""
                (function() {
                    const articles = document.querySelectorAll('article');
                    if (articles.length > 0) {
                        articles[articles.length - 1].scrollIntoView({ behavior: 'instant', block: 'end' });
                    }
                })()
                """);

            // Check immediately (before DOM resets)
            log.info("Immediately after scroll to last article:");
            logPageState(page);
            Object newScrollHeight = page.evaluate("document.body.scrollHeight");
            log.info("scrollHeight after scroll: {}", newScrollHeight);

            // Wait 500ms and check again
            page.waitForTimeout(500);
            log.info("After 500ms:");
            logPageState(page);

            // Wait another 500ms
            page.waitForTimeout(500);
            log.info("After 1000ms:");
            logPageState(page);

            // Scroll back to top
            page.evaluate("window.scrollTo(0, 0)");
            page.waitForTimeout(2000);
            log.info("\n=== AFTER SCROLL TO TOP ===");
            logPageState(page);

            // Try scrolling to specific article
            log.info("\n=== SCROLL STRATEGY 2: Scroll to 5th article ===");
            int articleCount = page.locator("article").count();
            if (articleCount >= 5) {
                page.locator("article").nth(4).scrollIntoViewIfNeeded();
                page.waitForTimeout(1500);
                log.info("After scrolling to 5th article:");
                logPageState(page);
            }

            // Try keyboard scroll (Page Down)
            log.info("\n=== SCROLL STRATEGY 3: Page Down key ===");
            page.evaluate("window.scrollTo(0, 0)");
            page.waitForTimeout(1000);
            for (int i = 1; i <= 3; i++) {
                page.keyboard().press("PageDown");
                page.waitForTimeout(1500);
                log.info("After PageDown {}:", i);
                logPageState(page);
            }

            // Check if there's a "Load more" or infinite scroll trigger
            log.info("\n=== LOOKING FOR LOAD MORE TRIGGERS ===");
            String[] loadMoreSelectors = {
                "button:has-text('Load more')",
                "button:has-text('Show more')",
                "[class*='loadMore']",
                "[class*='showMore']",
                "[data-testid*='load']",
                "[role='button']:has-text('more')"
            };

            for (String selector : loadMoreSelectors) {
                int count = page.locator(selector).count();
                if (count > 0) {
                    log.info("Found '{}': {} elements", selector, count);
                }
            }

            // Check page scroll metrics
            log.info("\n=== PAGE SCROLL METRICS ===");
            Object scrollHeight = page.evaluate("document.body.scrollHeight");
            Object scrollTop = page.evaluate("window.scrollY");
            Object clientHeight = page.evaluate("document.documentElement.clientHeight");
            log.info("scrollHeight: {}", scrollHeight);
            log.info("scrollTop: {}", scrollTop);
            log.info("clientHeight: {}", clientHeight);

        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
        } finally {
            page.close();
            context.close();
        }
    }

    private void logPageState(Page page) {
        int articleCount = page.locator("article").count();
        log.info("  article count: {}", articleCount);
        log.info("  URL: {}", page.url());

        if (articleCount > 0) {
            // Log first and last article titles
            try {
                String firstTitle = page.locator("article").first().locator("h2").first().textContent();
                log.info("  first article: {}", truncate(firstTitle, 50));

                if (articleCount > 1) {
                    String lastTitle = page.locator("article").last().locator("h2").first().textContent();
                    log.info("  last article: {}", truncate(lastTitle, 50));
                }
            } catch (Exception e) {
                log.info("  (could not get article titles)");
            }
        }

        Object scrollY = page.evaluate("window.scrollY");
        log.info("  scrollY: {}", scrollY);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "null";
        text = text.trim();
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
