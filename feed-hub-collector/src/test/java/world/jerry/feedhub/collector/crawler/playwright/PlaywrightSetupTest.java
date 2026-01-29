package world.jerry.feedhub.collector.crawler.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Playwright setup verification test
 * - First run will automatically download Chromium browser
 * - Tests basic browser functionality
 */
@SpringBootTest
class PlaywrightSetupTest {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightSetupTest.class);

    @Autowired
    private Browser browser;

    @Test
    void testBrowserInitialization() {
        log.info("Testing Playwright browser initialization");

        assertNotNull(browser, "Browser should be autowired");
        assertTrue(browser.isConnected(), "Browser should be connected");

        log.info("✓ Browser initialized successfully");
    }

    @Test
    void testBasicNavigation() {
        log.info("Testing basic page navigation");

        Page page = browser.newPage();

        try {
            // Test with simple site
            Response response = page.navigate("https://example.com");

            assertNotNull(response, "Response should not be null");
            assertTrue(response.ok(), "Response should be OK (2xx)");
            assertEquals(200, response.status(), "Should return HTTP 200");

            String title = page.title();
            assertNotNull(title, "Page title should not be null");
            assertFalse(title.isEmpty(), "Page title should not be empty");

            log.info("✓ Navigation successful");
            log.info("  Page title: {}", title);
            log.info("  HTTP Status: {}", response.status());

        } finally {
            page.close();
        }
    }
}
