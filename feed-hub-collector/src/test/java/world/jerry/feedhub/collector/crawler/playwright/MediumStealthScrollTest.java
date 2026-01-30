package world.jerry.feedhub.collector.crawler.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.HashMap;

/**
 * Test with enhanced stealth techniques to bypass Medium's bot detection
 * - Removes webdriver detection
 * - Applies fingerprinting evasion
 * - Monitors GraphQL request status after scroll
 */
class MediumStealthScrollTest {

    private static final Logger log = LoggerFactory.getLogger(MediumStealthScrollTest.class);

    private static final String DAANGN_URL = "https://medium.com/daangn/all";

    /**
     * Create HTTP headers that look like a real browser
     */
    private static Map<String, String> createHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
        headers.put("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("sec-ch-ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"");
        headers.put("sec-ch-ua-mobile", "?0");
        headers.put("sec-ch-ua-platform", "\"macOS\"");
        headers.put("sec-fetch-dest", "document");
        headers.put("sec-fetch-mode", "navigate");
        headers.put("sec-fetch-site", "none");
        headers.put("sec-fetch-user", "?1");
        headers.put("upgrade-insecure-requests", "1");
        return headers;
    }

    /**
     * Stealth JavaScript to inject before page load
     * - Removes navigator.webdriver property
     * - Modifies navigator properties to look like real browser
     */
    private static final String STEALTH_SCRIPT = """
        // Remove webdriver property
        Object.defineProperty(navigator, 'webdriver', {
            get: () => undefined
        });

        // Override navigator.plugins (Chrome has plugins)
        Object.defineProperty(navigator, 'plugins', {
            get: () => {
                const plugins = [
                    { name: 'Chrome PDF Plugin', filename: 'internal-pdf-viewer', description: 'Portable Document Format' },
                    { name: 'Chrome PDF Viewer', filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai', description: '' },
                    { name: 'Native Client', filename: 'internal-nacl-plugin', description: '' }
                ];
                plugins.item = (i) => plugins[i] || null;
                plugins.namedItem = (name) => plugins.find(p => p.name === name) || null;
                plugins.refresh = () => {};
                return plugins;
            }
        });

        // Override navigator.languages
        Object.defineProperty(navigator, 'languages', {
            get: () => ['ko-KR', 'ko', 'en-US', 'en']
        });

        // Override navigator.platform
        Object.defineProperty(navigator, 'platform', {
            get: () => 'MacIntel'
        });

        // Override navigator.hardwareConcurrency
        Object.defineProperty(navigator, 'hardwareConcurrency', {
            get: () => 8
        });

        // Override navigator.deviceMemory
        Object.defineProperty(navigator, 'deviceMemory', {
            get: () => 8
        });

        // Chrome-specific: add chrome object
        window.chrome = {
            runtime: {},
            loadTimes: function() {},
            csi: function() {},
            app: {}
        };

        // Override permissions query
        const originalQuery = window.navigator.permissions.query;
        window.navigator.permissions.query = (parameters) => (
            parameters.name === 'notifications' ?
                Promise.resolve({ state: Notification.permission }) :
                originalQuery(parameters)
        );
        """;

    @Test
    void testStealthScrollWithGraphQLMonitoring() {
        log.info("=== Testing with STEALTH browser settings ===");

        try (Playwright playwright = Playwright.create()) {
            // Launch browser with enhanced stealth options
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)  // Still headless, but with stealth
                    .setArgs(Arrays.asList(
                            "--disable-blink-features=AutomationControlled",
                            "--disable-dev-shm-usage",
                            "--no-sandbox",
                            "--disable-infobars",
                            "--disable-extensions",
                            "--disable-gpu",
                            "--disable-setuid-sandbox",
                            "--window-size=1920,1080"
                    ))
            );

            // Create context with full browser fingerprint
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080)
                    .setLocale("ko-KR")
                    .setTimezoneId("Asia/Seoul")
                    .setDeviceScaleFactor(2.0)
                    .setHasTouch(false)
                    .setJavaScriptEnabled(true)
                    .setExtraHTTPHeaders(createHeaders())
            );

            // Inject stealth script before any page loads
            context.addInitScript(STEALTH_SCRIPT);

            Page page = context.newPage();

            // Track GraphQL responses
            List<String> graphqlResponses = new ArrayList<>();
            page.onResponse(response -> {
                String url = response.url();
                if (url.contains("graphql")) {
                    String status = String.format("HTTP %d - %s", response.status(), url);
                    graphqlResponses.add(status);
                    log.info("GraphQL Response: {}", status);
                }
            });

            try {
                // Navigate to page
                log.info("Navigating to: {}", DAANGN_URL);
                Response response = page.navigate(DAANGN_URL, new Page.NavigateOptions()
                        .setTimeout(30000)
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                );

                log.info("Initial page load: HTTP {}", response != null ? response.status() : "null");

                // Wait for initial content
                page.waitForTimeout(3000);

                // Check stealth effectiveness
                log.info("\n=== Checking stealth effectiveness ===");
                Object webdriver = page.evaluate("navigator.webdriver");
                Object plugins = page.evaluate("navigator.plugins.length");
                Object languages = page.evaluate("navigator.languages");
                log.info("navigator.webdriver: {}", webdriver);
                log.info("navigator.plugins.length: {}", plugins);
                log.info("navigator.languages: {}", languages);

                // Initial state
                log.info("\n=== INITIAL STATE ===");
                int initialCount = page.locator("article").count();
                log.info("Article count: {}", initialCount);
                log.info("GraphQL responses: {}", graphqlResponses.size());

                // Perform scroll
                log.info("\n=== SCROLLING ===");
                for (int i = 1; i <= 3; i++) {
                    // Scroll to last article
                    page.evaluate("""
                        (function() {
                            const articles = document.querySelectorAll('article');
                            if (articles.length > 0) {
                                articles[articles.length - 1].scrollIntoView({ behavior: 'smooth', block: 'end' });
                            }
                            window.scrollBy(0, 300);
                        })()
                        """);

                    // Wait for GraphQL request
                    page.waitForTimeout(2500);

                    int count = page.locator("article").count();
                    log.info("After scroll {}: {} articles, {} GraphQL responses",
                            i, count, graphqlResponses.size());

                    // Log any 403 responses
                    for (String resp : graphqlResponses) {
                        if (resp.contains("403")) {
                            log.warn("BLOCKED: {}", resp);
                        }
                    }
                }

                // Final state
                log.info("\n=== FINAL STATE ===");
                int finalCount = page.locator("article").count();
                log.info("Final article count: {}", finalCount);
                log.info("Total GraphQL responses: {}", graphqlResponses.size());

                // Count 403 responses
                long blocked = graphqlResponses.stream().filter(r -> r.contains("403")).count();
                long success = graphqlResponses.stream().filter(r -> r.contains("200")).count();
                log.info("GraphQL 200 (success): {}", success);
                log.info("GraphQL 403 (blocked): {}", blocked);

                // List article titles
                if (finalCount > 0) {
                    log.info("\n=== Article Titles ===");
                    Locator articles = page.locator("article");
                    for (int i = 0; i < Math.min(finalCount, 15); i++) {
                        try {
                            String title = articles.nth(i).locator("h2").first().textContent();
                            log.info("  {}: {}", i + 1, title.trim().substring(0, Math.min(50, title.length())));
                        } catch (Exception e) {
                            log.info("  {}: (failed to get title)", i + 1);
                        }
                    }
                }

            } finally {
                page.close();
                context.close();
                browser.close();
            }
        }
    }

    @Test
    void testHeadfulStealthScroll() {
        log.info("=== Testing with HEADFUL + STEALTH (visible browser) ===");

        try (Playwright playwright = Playwright.create()) {
            // Launch HEADFUL browser with stealth
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(false)  // HEADFUL - visible window
                    .setSlowMo(50)       // Slow down for visibility
                    .setArgs(Arrays.asList(
                            "--disable-blink-features=AutomationControlled",
                            "--disable-infobars"
                    ))
            );

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080)
                    .setLocale("ko-KR")
                    .setTimezoneId("Asia/Seoul")
                    .setExtraHTTPHeaders(Map.of(
                            "Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
                            "sec-ch-ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"",
                            "sec-ch-ua-mobile", "?0",
                            "sec-ch-ua-platform", "\"macOS\""
                    ))
            );

            // Inject stealth script
            context.addInitScript(STEALTH_SCRIPT);

            Page page = context.newPage();

            // Track GraphQL
            List<Integer> graphqlStatuses = new ArrayList<>();
            page.onResponse(response -> {
                if (response.url().contains("graphql")) {
                    graphqlStatuses.add(response.status());
                    log.info("GraphQL: HTTP {} - {}", response.status(),
                            response.url().substring(0, Math.min(80, response.url().length())));
                }
            });

            try {
                page.navigate(DAANGN_URL, new Page.NavigateOptions()
                        .setTimeout(30000)
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                );

                page.waitForTimeout(3000);

                log.info("\n=== Initial: {} articles ===", page.locator("article").count());

                // Scroll multiple times
                for (int i = 1; i <= 5; i++) {
                    page.evaluate("""
                        (function() {
                            const articles = document.querySelectorAll('article');
                            if (articles.length > 0) {
                                articles[articles.length - 1].scrollIntoView({ behavior: 'smooth', block: 'end' });
                            }
                        })()
                        """);

                    page.waitForTimeout(2000);

                    log.info("Scroll {}: {} articles, GraphQL statuses: {}",
                            i, page.locator("article").count(), graphqlStatuses);
                }

                log.info("\n=== RESULT ===");
                log.info("Final articles: {}", page.locator("article").count());
                log.info("GraphQL 403 count: {}", graphqlStatuses.stream().filter(s -> s == 403).count());
                log.info("GraphQL 200 count: {}", graphqlStatuses.stream().filter(s -> s == 200).count());

                // Keep browser open briefly to see result
                page.waitForTimeout(3000);

            } finally {
                page.close();
                context.close();
                browser.close();
            }
        }
    }
}
