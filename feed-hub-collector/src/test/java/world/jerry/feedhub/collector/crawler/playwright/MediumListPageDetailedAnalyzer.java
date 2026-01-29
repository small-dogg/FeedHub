package world.jerry.feedhub.collector.crawler.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Detailed analysis of Medium list page to find author and date selectors
 */
@SpringBootTest
class MediumListPageDetailedAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(MediumListPageDetailedAnalyzer.class);

    @Autowired
    private Browser browser;

    private static final String MEDIUM_BLOG_URL = "https://blog.medium.com";

    @Test
    void analyzeAuthorAndDateSelectors() {
        log.info("=== Detailed Analysis for Author and Date ===");

        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setViewportSize(1920, 1080)
        );

        Page page = context.newPage();

        try {
            page.navigate(MEDIUM_BLOG_URL, new Page.NavigateOptions()
                    .setTimeout(30000)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            // Wait for JS rendering
            page.waitForTimeout(3000);

            // Get first article's full inner text to understand structure
            log.info("\n=== First article inner text ===");
            Locator firstArticle = page.locator("article").first();
            String innerText = firstArticle.innerText();
            log.info("InnerText:\n{}", innerText);

            // Look for all text patterns that might contain author/date
            log.info("\n=== Looking for author/date patterns in all articles ===");

            int articleCount = page.locator("article").count();
            for (int i = 0; i < Math.min(articleCount, 2); i++) {
                Locator article = page.locator("article").nth(i);
                log.info("\n--- Article {} ---", i + 1);

                // Get h2 (title)
                String title = article.locator("h2").first().textContent();
                log.info("Title: {}", truncate(title, 50));

                // Look for all anchor tags (might contain author)
                Locator links = article.locator("a");
                int linkCount = links.count();
                log.info("Found {} links in article", linkCount);

                for (int j = 0; j < linkCount; j++) {
                    Locator link = links.nth(j);
                    String href = link.getAttribute("href");
                    String text = link.textContent().trim();

                    // Author links typically contain @username or /@ pattern
                    if (href != null && (href.contains("/@") || href.contains("/u/"))) {
                        log.info("  Potential Author Link: href={}, text={}", href, text);
                    }
                }

                // Look for all span elements (often used for metadata)
                Locator spans = article.locator("span");
                int spanCount = spans.count();
                log.info("Found {} spans in article", spanCount);

                for (int j = 0; j < Math.min(spanCount, 20); j++) {
                    Locator span = spans.nth(j);
                    String text = span.textContent().trim();

                    // Look for date patterns (min, read, ago, 2024, 2025, Jan, Feb, etc.)
                    if (text.matches(".*\\d+ min.*") ||
                            text.matches(".*read.*") ||
                            text.matches(".*ago.*") ||
                            text.matches(".*202[0-9].*") ||
                            text.matches(".*(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec).*")) {
                        log.info("  Potential Date span: {}", text);
                    }
                }

                // Look for div elements that might contain author/date
                Locator divs = article.locator("div");
                log.info("Found {} divs in article", divs.count());

                // Look for specific data attributes
                Locator elementsWithDataTestId = article.locator("[data-testid]");
                int dataTestIdCount = elementsWithDataTestId.count();
                log.info("Found {} elements with data-testid", dataTestIdCount);

                for (int j = 0; j < dataTestIdCount; j++) {
                    Locator el = elementsWithDataTestId.nth(j);
                    String testId = el.getAttribute("data-testid");
                    String text = el.textContent().trim();
                    log.info("  data-testid='{}': {}", testId, truncate(text, 50));
                }
            }

            log.info("\n=== Analysis complete ===");

        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
        } finally {
            page.close();
            context.close();
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        text = text.replaceAll("\\s+", " ").trim();
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
