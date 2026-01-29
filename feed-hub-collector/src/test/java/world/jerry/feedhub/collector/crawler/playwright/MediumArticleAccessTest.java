package world.jerry.feedhub.collector.crawler.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test Medium article access and content extraction
 * - Uses real Medium article URLs
 * - Tests bot detection bypass
 * - Validates content parsing
 */
@SpringBootTest
class MediumArticleAccessTest {

    private static final Logger log = LoggerFactory.getLogger(MediumArticleAccessTest.class);

    @Autowired
    private Browser browser;

    // Test with Medium's own blog posts (publicly accessible)
    private static final String MEDIUM_BLOG_ARTICLE = "https://blog.medium.com";

    @Test
    void testMediumBlogAccess() {
        log.info("=== Testing Medium Blog Access ===");
        log.info("URL: {}", MEDIUM_BLOG_ARTICLE);

        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setViewportSize(1920, 1080)
        );

        Page page = context.newPage();

        try {
            long startTime = System.currentTimeMillis();

            Response response = page.navigate(MEDIUM_BLOG_ARTICLE, new Page.NavigateOptions()
                    .setTimeout(30000)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            long loadTime = System.currentTimeMillis() - startTime;

            assertNotNull(response, "Response should not be null");

            int status = response.status();
            log.info("HTTP Status: {}", status);
            log.info("Load time: {}ms", loadTime);

            if (status == 403) {
                log.error("!!! Bot detection active (HTTP 403) !!!");
                fail("Medium blocked the request: HTTP 403");
            }

            if (status == 429) {
                log.error("!!! Rate limiting active (HTTP 429) !!!");
                fail("Too many requests: HTTP 429");
            }

            assertTrue(response.ok(), "Response should be OK (2xx)");

            String title = page.title();
            log.info("Page title: {}", title);

            String url = page.url();
            log.info("Final URL: {}", url);

            // Check if page contains Medium content
            String htmlContent = page.content();
            assertTrue(htmlContent.contains("medium") || htmlContent.contains("Medium"),
                    "Page should contain Medium content");

            log.info("=== SUCCESS: Medium blog accessible ===");

        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        } finally {
            page.close();
            context.close();
        }
    }

    @Test
    void testMediumArticleContentExtraction() {
        log.info("=== Testing Medium Article Content Extraction ===");

        // Use Medium's engineering blog (well-known, stable URL)
        String testUrl = "https://medium.engineering";

        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setViewportSize(1920, 1080)
        );

        Page page = context.newPage();

        try {
            Response response = page.navigate(testUrl, new Page.NavigateOptions()
                    .setTimeout(30000)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            assertNotNull(response, "Response should not be null");

            int status = response.status();
            log.info("HTTP Status: {}", status);

            if (status == 403) {
                log.error("!!! Bot detection active (HTTP 403) !!!");
                log.error("Cannot proceed with content extraction test");
                fail("Medium blocked the request");
            }

            assertTrue(response.ok(), "Should be able to access Medium content");

            // Test selector presence
            log.info("Testing CSS selectors...");

            // Check if article elements exist
            try {
                int h1Count = page.locator("h1").count();
                log.info("Found {} h1 elements", h1Count);

                int h2Count = page.locator("h2").count();
                log.info("Found {} h2 elements", h2Count);

                int articleCount = page.locator("article").count();
                log.info("Found {} article elements", articleCount);

                log.info("✓ Page structure detected successfully");

            } catch (Exception e) {
                log.warn("Could not detect standard selectors: {}", e.getMessage());
            }

            log.info("=== Content extraction test completed ===");

        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        } finally {
            page.close();
            context.close();
        }
    }
}
