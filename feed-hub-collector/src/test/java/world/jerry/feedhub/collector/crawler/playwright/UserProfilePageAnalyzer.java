package world.jerry.feedhub.collector.crawler.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Analyze User Profile page structure to fix extraction
 */
@SpringBootTest
class UserProfilePageAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(UserProfilePageAnalyzer.class);

    @Autowired
    private Browser browser;

    private static final String USER_PROFILE_URL = "https://medium.com/@springrod";

    @Test
    void analyzeUserProfilePage() {
        log.info("=== Analyzing User Profile Page ===");

        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .setViewportSize(1920, 1080)
        );

        Page page = context.newPage();

        try {
            page.navigate(USER_PROFILE_URL, new Page.NavigateOptions()
                    .setTimeout(30000)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            page.waitForTimeout(2000);

            // Analyze first article structure
            log.info("\n=== First Article Inner HTML (truncated) ===");
            Locator firstArticle = page.locator("article").first();
            String outerHtml = firstArticle.evaluate("el => el.outerHTML").toString();
            log.info(truncate(outerHtml, 3000));

            // Analyze all links in first article
            log.info("\n=== All links in first article ===");
            Locator links = firstArticle.locator("a");
            int linkCount = links.count();
            log.info("Found {} links", linkCount);

            for (int i = 0; i < linkCount; i++) {
                Locator link = links.nth(i);
                String href = link.getAttribute("href");
                String text = link.textContent().trim().replaceAll("\\s+", " ");
                log.info("  Link {}: href={}", i, truncate(href, 80));
                log.info("         text={}", truncate(text, 50));
            }

            // Find article link pattern
            log.info("\n=== Looking for article link patterns ===");

            // Try data-testid for links
            Locator postLinks = firstArticle.locator("[data-testid*='post'] a, [data-testid*='card'] a");
            log.info("Found {} links with post/card data-testid", postLinks.count());

            // Try div > a pattern
            Locator divLinks = firstArticle.locator("div > a[href]");
            log.info("Found {} div > a links", divLinks.count());

            // Look for specific patterns
            Locator h2Links = firstArticle.locator("h2 a");
            log.info("h2 > a count: {}", h2Links.count());
            if (h2Links.count() > 0) {
                log.info("  h2 > a href: {}", h2Links.first().getAttribute("href"));
            }

            // Check if title itself is a link
            Locator h2 = firstArticle.locator("h2").first();
            log.info("\nH2 tag outerHTML: {}", truncate(h2.evaluate("el => el.outerHTML").toString(), 500));

        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
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
