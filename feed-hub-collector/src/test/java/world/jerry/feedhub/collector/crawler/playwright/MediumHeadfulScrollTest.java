package world.jerry.feedhub.collector.crawler.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Test with headful browser (visible window) to compare with headless
 * - Check if Medium blocks headless browsers
 * - Monitor GraphQL network requests
 */
class MediumHeadfulScrollTest {

    private static final Logger log = LoggerFactory.getLogger(MediumHeadfulScrollTest.class);

    private static final String DAANGN_URL = "https://medium.com/daangn/all";

    @Test
    void testHeadfulScrollWithNetworkMonitoring() {
        log.info("=== Testing with HEADFUL browser (visible window) ===");

        // Create Playwright with headful mode
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(false)  // HEADFUL mode - visible browser
                    .setSlowMo(100)      // Slow down for visibility
            );

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080)
                    .setLocale("ko-KR")
                    .setTimezoneId("Asia/Seoul")
                    .setExtraHTTPHeaders(java.util.Map.of(
                        "Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
                        "sec-ch-ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"",
                        "sec-ch-ua-mobile", "?0",
                        "sec-ch-ua-platform", "\"macOS\""
                    ))
            );

            Page page = context.newPage();

            // Monitor network requests for GraphQL
            List<String> graphqlRequests = new ArrayList<>();
            page.onRequest(request -> {
                String url = request.url();
                if (url.contains("graphql") || url.contains("_/graphql")) {
                    log.info("GraphQL Request: {}", url);
                    graphqlRequests.add(url);
                }
            });

            page.onResponse(response -> {
                String url = response.url();
                if (url.contains("graphql")) {
                    log.info("GraphQL Response: {} - {}", response.status(), url);
                }
            });

            try {
                page.navigate(DAANGN_URL, new Page.NavigateOptions()
                        .setTimeout(30000)
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                );

                page.waitForTimeout(3000);

                // Initial state
                log.info("\n=== INITIAL STATE (Headful) ===");
                int initialCount = page.locator("article").count();
                log.info("Article count: {}", initialCount);
                log.info("GraphQL requests so far: {}", graphqlRequests.size());

                // Scroll to last article
                log.info("\n=== Scrolling to last article ===");
                page.evaluate("""
                    (function() {
                        const articles = document.querySelectorAll('article');
                        if (articles.length > 0) {
                            articles[articles.length - 1].scrollIntoView({ behavior: 'smooth', block: 'end' });
                        }
                    })()
                    """);

                // Wait for potential GraphQL request
                page.waitForTimeout(3000);

                log.info("\n=== AFTER SCROLL (Headful) ===");
                int afterScrollCount = page.locator("article").count();
                log.info("Article count: {}", afterScrollCount);
                log.info("GraphQL requests: {}", graphqlRequests.size());

                // Scroll more
                log.info("\n=== Scrolling more ===");
                page.evaluate("window.scrollBy(0, 500)");
                page.waitForTimeout(3000);

                int finalCount = page.locator("article").count();
                log.info("Final article count: {}", finalCount);
                log.info("Total GraphQL requests: {}", graphqlRequests.size());

                // List all titles
                if (finalCount > 0) {
                    log.info("\n=== All article titles ===");
                    Locator articles = page.locator("article");
                    for (int i = 0; i < Math.min(finalCount, 20); i++) {
                        try {
                            String title = articles.nth(i).locator("h2").first().textContent();
                            log.info("  {}: {}", i + 1, title.trim().substring(0, Math.min(50, title.length())));
                        } catch (Exception e) {
                            log.info("  {}: (failed to get title)", i + 1);
                        }
                    }
                }

            } finally {
                // Keep browser open for a moment to see results
                page.waitForTimeout(2000);
                page.close();
                context.close();
                browser.close();
            }
        }
    }
}
