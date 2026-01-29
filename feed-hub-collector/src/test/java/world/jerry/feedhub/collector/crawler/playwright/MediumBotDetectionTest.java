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
 * Critical test to verify if Playwright can bypass Medium's bot detection
 * - Tests actual Medium article access
 * - Validates HTTP response status
 * - Determines if Playwright approach is viable
 *
 * THIS IS THE MOST IMPORTANT TEST - determines Go/No-Go for Playwright integration
 */
@SpringBootTest
class MediumBotDetectionTest {

    private static final Logger log = LoggerFactory.getLogger(MediumBotDetectionTest.class);

    @Autowired
    private Browser browser;

    // Well-known Medium URL for testing
    // Using official Medium blog that should be publicly accessible
    private static final String TEST_MEDIUM_URL = "https://blog.medium.com";

    @Test
    void testMediumAccessWithPlaywright() {
        log.info("=== CRITICAL TEST: Medium Bot Detection Bypass ===");
        log.info("Testing URL: {}", TEST_MEDIUM_URL);

        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setViewportSize(1920, 1080)
        );

        Page page = context.newPage();

        try {
            long startTime = System.currentTimeMillis();

            // Use DOMCONTENTLOADED instead of NETWORKIDLE
            // (Medium pages have continuous network activity that prevents NETWORKIDLE)
            Response response = page.navigate(TEST_MEDIUM_URL, new Page.NavigateOptions()
                    .setTimeout(30000)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            long loadTime = System.currentTimeMillis() - startTime;

            assertNotNull(response, "Response should not be null");

            int status = response.status();
            log.info("HTTP Status: {}", status);
            log.info("Load time: {}ms", loadTime);

            // Critical assertion: should NOT be 403 (Forbidden)
            if (status == 403) {
                log.error("!!! FAILED: Bot detection active (HTTP 403) !!!");
                log.error("Medium is blocking Playwright requests");
                log.error("Recommendation: Use RSS feed sync only, abandon web scraping");
                fail("Bot detection not bypassed: HTTP 403");
            }

            if (status == 429) {
                log.error("!!! Rate limiting active (HTTP 429) !!!");
                fail("Too many requests: HTTP 429");
            }

            // Should be successful (2xx)
            assertTrue(response.ok(), "Response should be OK (2xx), got: " + status);

            String title = page.title();
            log.info("Page title: {}", title);

            // Validate page loaded correctly
            assertNotNull(title, "Page title should not be null");
            assertFalse(title.isEmpty(), "Page title should not be empty");

            log.info("=== SUCCESS: Bot detection bypassed! ===");
            log.info("Playwright approach is VIABLE for Medium scraping");

        } catch (AssertionError e) {
            // Re-throw assertion errors
            throw e;
        } catch (Exception e) {
            log.error("Error during Medium access test: {}", e.getMessage(), e);
            fail("Test failed with exception: " + e.getMessage());
        } finally {
            page.close();
            context.close();
        }
    }

    @Test
    void testMediumArticlePageStructure() {
        log.info("Testing Medium article page structure detection");

        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setViewportSize(1920, 1080)
        );

        Page page = context.newPage();

        try {
            // Test with Medium homepage first
            Response response = page.navigate("https://medium.com", new Page.NavigateOptions()
                    .setTimeout(30000)
            );

            assertNotNull(response, "Response should not be null");

            int status = response.status();
            log.info("Medium homepage HTTP Status: {}", status);

            if (status == 403) {
                log.error("!!! Medium homepage blocked (HTTP 403) !!!");
                fail("Bot detection active on Medium homepage");
            }

            assertTrue(response.ok(), "Medium homepage should be accessible");
            log.info("✓ Medium homepage accessible (HTTP {})", status);

        } finally {
            page.close();
            context.close();
        }
    }
}
