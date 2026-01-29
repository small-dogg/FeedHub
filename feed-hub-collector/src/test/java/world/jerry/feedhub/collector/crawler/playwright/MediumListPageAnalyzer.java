package world.jerry.feedhub.collector.crawler.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * Analyze Medium list page structure to find optimal selectors
 * for extracting article cards (title, summary, author, date, link)
 */
@SpringBootTest
class MediumListPageAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(MediumListPageAnalyzer.class);

    @Autowired
    private Browser browser;

    // Test with Medium's official blog list page
    private static final String MEDIUM_BLOG_URL = "https://blog.medium.com";

    @Test
    void analyzeListPageStructure() {
        log.info("=== Analyzing Medium List Page Structure ===");
        log.info("URL: {}", MEDIUM_BLOG_URL);

        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setViewportSize(1920, 1080)
        );

        Page page = context.newPage();

        try {
            Response response = page.navigate(MEDIUM_BLOG_URL, new Page.NavigateOptions()
                    .setTimeout(30000)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            log.info("HTTP Status: {}", response.status());

            // Wait for content to render
            page.waitForTimeout(3000);

            // Analyze article card structure
            log.info("\n=== Looking for article card patterns ===");

            // Try common article card selectors
            String[] cardSelectors = {
                    "article",
                    "[data-testid='postPreview']",
                    "[data-testid='post-preview']",
                    "div[class*='post']",
                    "div[class*='card']",
                    "div[class*='article']",
                    "a[data-action='open-post']",
                    "div[class*='streamItem']"
            };

            for (String selector : cardSelectors) {
                int count = page.locator(selector).count();
                if (count > 0) {
                    log.info("Found {} elements with selector: {}", count, selector);
                }
            }

            // Analyze article elements
            log.info("\n=== Analyzing article elements ===");
            int articleCount = page.locator("article").count();
            log.info("Found {} <article> elements", articleCount);

            if (articleCount > 0) {
                for (int i = 0; i < Math.min(articleCount, 3); i++) {
                    Locator article = page.locator("article").nth(i);
                    log.info("\n--- Article {} ---", i + 1);

                    // Find title (h1, h2, h3)
                    for (String headingSelector : List.of("h1", "h2", "h3")) {
                        Locator heading = article.locator(headingSelector).first();
                        if (heading.count() > 0) {
                            String text = heading.textContent().trim();
                            if (!text.isEmpty()) {
                                log.info("  {} (title): {}", headingSelector, truncate(text, 60));
                            }
                        }
                    }

                    // Find link
                    Locator links = article.locator("a[href*='medium.com'], a[href^='/']");
                    if (links.count() > 0) {
                        String href = links.first().getAttribute("href");
                        log.info("  Link: {}", href);
                    }

                    // Find author
                    for (String authorSelector : List.of(
                            "[data-testid='authorName']",
                            "a[rel='author']",
                            "div[class*='author']",
                            "span[class*='author']",
                            "p[class*='author']")) {
                        Locator author = article.locator(authorSelector).first();
                        if (author.count() > 0) {
                            String text = author.textContent().trim();
                            if (!text.isEmpty()) {
                                log.info("  Author ({}): {}", authorSelector, text);
                            }
                        }
                    }

                    // Find date
                    for (String dateSelector : List.of(
                            "time",
                            "time[datetime]",
                            "span[class*='date']",
                            "div[class*='date']",
                            "span[class*='time']")) {
                        Locator date = article.locator(dateSelector).first();
                        if (date.count() > 0) {
                            String datetime = date.getAttribute("datetime");
                            String text = date.textContent().trim();
                            if (datetime != null) {
                                log.info("  Date ({}): datetime={}", dateSelector, datetime);
                            } else if (!text.isEmpty()) {
                                log.info("  Date ({}): text={}", dateSelector, text);
                            }
                        }
                    }

                    // Find summary/description
                    for (String summarySelector : List.of(
                            "p",
                            "div[class*='preview']",
                            "div[class*='summary']",
                            "div[class*='subtitle']",
                            "h3 + p",
                            "h2 + p")) {
                        Locator summary = article.locator(summarySelector).first();
                        if (summary.count() > 0) {
                            String text = summary.textContent().trim();
                            if (!text.isEmpty() && text.length() > 20) {
                                log.info("  Summary ({}): {}", summarySelector, truncate(text, 80));
                                break; // Found one
                            }
                        }
                    }
                }
            }

            // Print raw HTML structure for first article (for manual inspection)
            log.info("\n=== First article HTML structure ===");
            if (articleCount > 0) {
                String outerHtml = page.locator("article").first().evaluate("el => el.outerHTML").toString();
                log.info("First article HTML (truncated to 2000 chars):\n{}", truncate(outerHtml, 2000));
            }

            log.info("\n=== Analysis complete ===");

        } catch (Exception e) {
            log.error("Error during analysis: {}", e.getMessage(), e);
        } finally {
            page.close();
            context.close();
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
