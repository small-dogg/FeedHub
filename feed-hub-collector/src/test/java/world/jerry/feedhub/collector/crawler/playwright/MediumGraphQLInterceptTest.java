package world.jerry.feedhub.collector.crawler.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Test GraphQL request interception to bypass Medium's bot detection
 * - Intercept GraphQL requests and add required headers
 * - Route requests through modification
 */
class MediumGraphQLInterceptTest {

    private static final Logger log = LoggerFactory.getLogger(MediumGraphQLInterceptTest.class);

    private static final String DAANGN_URL = "https://medium.com/daangn/all";

    private static final String STEALTH_SCRIPT = """
        Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
        Object.defineProperty(navigator, 'plugins', {
            get: () => {
                const plugins = [
                    { name: 'Chrome PDF Plugin' },
                    { name: 'Chrome PDF Viewer' },
                    { name: 'Native Client' }
                ];
                plugins.item = (i) => plugins[i] || null;
                plugins.namedItem = (name) => plugins.find(p => p.name === name) || null;
                return plugins;
            }
        });
        Object.defineProperty(navigator, 'languages', { get: () => ['ko-KR', 'ko', 'en-US', 'en'] });
        window.chrome = { runtime: {} };
        """;

    @Test
    void testGraphQLRequestInterception() {
        log.info("=== Testing GraphQL Request Interception ===");

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(Arrays.asList(
                            "--disable-blink-features=AutomationControlled",
                            "--disable-dev-shm-usage"
                    ))
            );

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080)
                    .setLocale("ko-KR")
                    .setTimezoneId("Asia/Seoul")
            );

            context.addInitScript(STEALTH_SCRIPT);

            Page page = context.newPage();

            // Enable request interception for GraphQL
            page.route("**/_/graphql**", route -> {
                Request request = route.request();
                Map<String, String> headers = new HashMap<>(request.headers());

                // Add headers that real Chrome sends
                headers.put("accept", "*/*");
                headers.put("content-type", "application/json");
                headers.put("origin", "https://medium.com");
                headers.put("referer", DAANGN_URL);
                headers.put("sec-fetch-dest", "empty");
                headers.put("sec-fetch-mode", "cors");
                headers.put("sec-fetch-site", "same-origin");

                // Log intercepted request
                log.info("Intercepting GraphQL request with modified headers");

                route.resume(new Route.ResumeOptions().setHeaders(headers));
            });

            // Track responses
            List<String> graphqlResults = new ArrayList<>();
            page.onResponse(response -> {
                if (response.url().contains("graphql")) {
                    graphqlResults.add("HTTP " + response.status());
                    log.info("GraphQL Response: HTTP {}", response.status());
                }
            });

            try {
                log.info("Navigating to: {}", DAANGN_URL);
                Response response = page.navigate(DAANGN_URL, new Page.NavigateOptions()
                        .setTimeout(30000)
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                );

                log.info("Initial load: HTTP {}", response != null ? response.status() : "null");
                page.waitForTimeout(3000);

                log.info("Initial articles: {}", page.locator("article").count());
                log.info("GraphQL responses: {}", graphqlResults);

                // Scroll
                log.info("\n=== Scrolling ===");
                for (int i = 1; i <= 3; i++) {
                    page.evaluate("""
                        const articles = document.querySelectorAll('article');
                        if (articles.length > 0) {
                            articles[articles.length - 1].scrollIntoView({ behavior: 'smooth', block: 'end' });
                        }
                        """);

                    page.waitForTimeout(2500);
                    log.info("Scroll {}: {} articles", i, page.locator("article").count());
                }

                log.info("\n=== Results ===");
                log.info("Final articles: {}", page.locator("article").count());
                long blocked = graphqlResults.stream().filter(r -> r.contains("403")).count();
                long success = graphqlResults.stream().filter(r -> r.contains("200")).count();
                log.info("GraphQL 200: {}, 403: {}", success, blocked);

            } finally {
                page.close();
                context.close();
                browser.close();
            }
        }
    }

    @Test
    void testWithCookiePersistence() {
        log.info("=== Testing with Cookie Persistence ===");

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(Arrays.asList(
                            "--disable-blink-features=AutomationControlled"
                    ))
            );

            // First: Load page and collect cookies
            BrowserContext context1 = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080)
            );
            context1.addInitScript(STEALTH_SCRIPT);

            Page page1 = context1.newPage();

            try {
                page1.navigate(DAANGN_URL, new Page.NavigateOptions()
                        .setTimeout(30000)
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                );
                page1.waitForTimeout(5000);

                // Get cookies
                List<Cookie> cookies = context1.cookies();
                log.info("Collected {} cookies", cookies.size());
                for (Cookie cookie : cookies) {
                    log.info("  Cookie: {} = {} (domain: {})",
                            cookie.name,
                            cookie.value.length() > 20 ? cookie.value.substring(0, 20) + "..." : cookie.value,
                            cookie.domain);
                }

                // Close first context
                page1.close();
                context1.close();

                // Second: Create new context with cookies
                log.info("\n=== Creating second context with saved cookies ===");
                BrowserContext context2 = browser.newContext(new Browser.NewContextOptions()
                        .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .setViewportSize(1920, 1080)
                );
                context2.addInitScript(STEALTH_SCRIPT);

                // Add cookies
                context2.addCookies(cookies);

                Page page2 = context2.newPage();

                List<Integer> graphqlStatuses = new ArrayList<>();
                page2.onResponse(response -> {
                    if (response.url().contains("graphql")) {
                        graphqlStatuses.add(response.status());
                        log.info("GraphQL: HTTP {}", response.status());
                    }
                });

                page2.navigate(DAANGN_URL, new Page.NavigateOptions()
                        .setTimeout(30000)
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                );
                page2.waitForTimeout(3000);

                log.info("Initial articles: {}", page2.locator("article").count());

                // Scroll with cookies
                for (int i = 1; i <= 3; i++) {
                    page2.evaluate("""
                        const articles = document.querySelectorAll('article');
                        if (articles.length > 0) {
                            articles[articles.length - 1].scrollIntoView({ behavior: 'smooth', block: 'end' });
                        }
                        """);
                    page2.waitForTimeout(2500);
                    log.info("Scroll {}: {} articles, GraphQL: {}",
                            i, page2.locator("article").count(), graphqlStatuses);
                }

                log.info("\n=== Results with Cookie Persistence ===");
                log.info("Final articles: {}", page2.locator("article").count());
                log.info("GraphQL 403 count: {}", graphqlStatuses.stream().filter(s -> s == 403).count());

                page2.close();
                context2.close();

            } finally {
                browser.close();
            }
        }
    }

    @Test
    void testWithSystemChrome() {
        log.info("=== Testing with System Chrome (if available) ===");

        // Check if system Chrome exists
        String[] chromePaths = {
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                "/usr/bin/google-chrome",
                "/usr/bin/chromium-browser"
        };

        String chromePath = null;
        for (String path : chromePaths) {
            if (new java.io.File(path).exists()) {
                chromePath = path;
                break;
            }
        }

        if (chromePath == null) {
            log.warn("System Chrome not found, skipping test");
            return;
        }

        log.info("Using system Chrome: {}", chromePath);

        try (Playwright playwright = Playwright.create()) {
            // Launch with system Chrome executable
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setExecutablePath(java.nio.file.Paths.get(chromePath))
                    .setHeadless(true)
                    .setArgs(Arrays.asList(
                            "--disable-blink-features=AutomationControlled"
                    ))
            );

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080)
            );
            context.addInitScript(STEALTH_SCRIPT);

            Page page = context.newPage();

            List<Integer> graphqlStatuses = new ArrayList<>();
            page.onResponse(response -> {
                if (response.url().contains("graphql")) {
                    graphqlStatuses.add(response.status());
                    log.info("GraphQL: HTTP {}", response.status());
                }
            });

            try {
                page.navigate(DAANGN_URL, new Page.NavigateOptions()
                        .setTimeout(30000)
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                );
                page.waitForTimeout(3000);

                log.info("Initial: {} articles", page.locator("article").count());

                for (int i = 1; i <= 3; i++) {
                    page.evaluate("""
                        const articles = document.querySelectorAll('article');
                        if (articles.length > 0) {
                            articles[articles.length - 1].scrollIntoView({ behavior: 'smooth', block: 'end' });
                        }
                        """);
                    page.waitForTimeout(2500);
                    log.info("Scroll {}: {} articles", i, page.locator("article").count());
                }

                log.info("\n=== System Chrome Results ===");
                log.info("Final: {} articles", page.locator("article").count());
                log.info("GraphQL 403: {}", graphqlStatuses.stream().filter(s -> s == 403).count());
                log.info("GraphQL 200: {}", graphqlStatuses.stream().filter(s -> s == 200).count());

            } finally {
                page.close();
                context.close();
                browser.close();
            }
        }
    }
}
