package world.jerry.feedhub.collector.crawler.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import world.jerry.feedhub.collector.domain.CrawledArticle;

import java.time.Instant;
import java.util.Arrays;

/**
 * Manual test for Medium scraping with Playwright
 * - Tests bot detection bypass
 * - Can be run as standalone Java application
 *
 * Usage:
 *   Update TEST_ARTICLE_URL with a real Medium article URL
 *   Run this main method to test scraping
 */
public class ManualMediumScraperTest {

    // Replace with actual Medium article URL for testing
    private static final String TEST_ARTICLE_URL = "https://medium.com/@example/sample-article";

    public static void main(String[] args) {
        System.out.println("=== Playwright Medium Scraper Manual Test ===\n");

        try (Playwright playwright = Playwright.create()) {
            System.out.println("Playwright initialized successfully");

            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(Arrays.asList(
                            "--disable-blink-features=AutomationControlled",
                            "--disable-dev-shm-usage",
                            "--no-sandbox"
                    ))
            );

            System.out.println("Chromium browser launched");

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080)
            );

            Page page = context.newPage();

            System.out.println("\nNavigating to: " + TEST_ARTICLE_URL);
            long startTime = System.currentTimeMillis();

            try {
                Response response = page.navigate(TEST_ARTICLE_URL, new Page.NavigateOptions()
                        .setTimeout(30000)
                        .setWaitUntil(WaitUntilState.NETWORKIDLE)
                );

                long loadTime = System.currentTimeMillis() - startTime;

                if (response == null) {
                    System.out.println("ERROR: No response received");
                } else {
                    int status = response.status();
                    System.out.println("HTTP Status: " + status);
                    System.out.println("Load time: " + loadTime + "ms");

                    if (status == 403) {
                        System.out.println("\n!!! FAILED: Bot detection active (HTTP 403) !!!");
                        System.out.println("Playwright approach is not viable for Medium");
                    } else if (status == 429) {
                        System.out.println("\n!!! FAILED: Rate limiting active (HTTP 429) !!!");
                    } else if (status >= 200 && status < 300) {
                        System.out.println("\n=== SUCCESS: Bot detection bypassed! ===");

                        // Wait for content to be rendered
                        try {
                            page.waitForSelector("article h1", new Page.WaitForSelectorOptions()
                                    .setTimeout(10000)
                            );
                            System.out.println("Article content loaded");
                        } catch (TimeoutError e) {
                            System.out.println("WARNING: Article title selector not found");
                        }

                        // Try to extract title
                        try {
                            String title = page.locator("article h1").first().textContent();
                            System.out.println("\nExtracted Title: " + title);
                        } catch (Exception e) {
                            System.out.println("WARNING: Could not extract title - " + e.getMessage());
                        }

                        // Try to extract content
                        try {
                            int paragraphCount = page.locator("article section p, article div p").count();
                            System.out.println("Found " + paragraphCount + " paragraphs");

                            if (paragraphCount > 0) {
                                String firstParagraph = page.locator("article section p, article div p").first().textContent();
                                String preview = firstParagraph.length() > 100
                                        ? firstParagraph.substring(0, 100) + "..."
                                        : firstParagraph;
                                System.out.println("First paragraph preview: " + preview);
                            }
                        } catch (Exception e) {
                            System.out.println("WARNING: Could not extract content - " + e.getMessage());
                        }

                        // Try to extract author
                        try {
                            String author = page.locator("[data-testid='authorName']").first().textContent();
                            System.out.println("Author: " + author);
                        } catch (Exception e) {
                            System.out.println("Author extraction failed (may not be available)");
                        }

                        // Try to extract published date
                        try {
                            String datetime = page.getAttribute("time[datetime]", "datetime");
                            if (datetime != null) {
                                System.out.println("Published: " + datetime);
                            }
                        } catch (Exception e) {
                            System.out.println("Date extraction failed (may not be available)");
                        }

                        System.out.println("\n=== Playwright Medium scraping is VIABLE ===");
                    } else {
                        System.out.println("\nUnexpected HTTP status: " + status);
                    }
                }

            } catch (Exception e) {
                System.out.println("\nERROR during scraping: " + e.getMessage());
                e.printStackTrace();
            } finally {
                page.close();
                context.close();
                browser.close();
            }

        } catch (Exception e) {
            System.out.println("\nFATAL ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n=== Test completed ===");
    }
}
