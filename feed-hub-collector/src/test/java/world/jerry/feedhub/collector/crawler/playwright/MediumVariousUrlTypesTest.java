package world.jerry.feedhub.collector.crawler.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import world.jerry.feedhub.collector.domain.CrawledArticle;

import java.util.List;

/**
 * Test various Medium URL types to ensure crawler compatibility
 */
@SpringBootTest
class MediumVariousUrlTypesTest {

    private static final Logger log = LoggerFactory.getLogger(MediumVariousUrlTypesTest.class);

    @Autowired
    private Browser browser;

    @Autowired
    private PlaywrightMediumScraper scraper;

    // Type 1: Publication /all page
    private static final String DAANGN_URL = "https://medium.com/daangn/all";
    private static final String COUPANG_URL = "https://medium.com/coupang-engineering/all";

    // Type 2: User profile
    private static final String USER_PROFILE_URL = "https://medium.com/@springrod";

    // Type 3: Custom domain (Kakao Tech - may not be Medium)
    private static final String KAKAO_TECH_URL = "https://tech.kakao.com/blog?page=1";

    @Test
    void testType1_PublicationAll_Daangn() {
        log.info("\n========================================");
        log.info("Type 1: Publication /all - Daangn");
        log.info("URL: {}", DAANGN_URL);
        log.info("========================================");

        analyzeAndScrape(DAANGN_URL);
    }

    @Test
    void testType1_PublicationAll_Coupang() {
        log.info("\n========================================");
        log.info("Type 1: Publication /all - Coupang Engineering");
        log.info("URL: {}", COUPANG_URL);
        log.info("========================================");

        analyzeAndScrape(COUPANG_URL);
    }

    @Test
    void testType2_UserProfile() {
        log.info("\n========================================");
        log.info("Type 2: User Profile");
        log.info("URL: {}", USER_PROFILE_URL);
        log.info("========================================");

        analyzeAndScrape(USER_PROFILE_URL);
    }

    @Test
    void testType3_CustomDomain_KakaoTech() {
        log.info("\n========================================");
        log.info("Type 3: Custom Domain - Kakao Tech");
        log.info("URL: {}", KAKAO_TECH_URL);
        log.info("========================================");

        analyzeCustomDomain(KAKAO_TECH_URL);
    }

    private void analyzeAndScrape(String url) {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .setViewportSize(1920, 1080)
        );

        Page page = context.newPage();

        try {
            Response response = page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(30000)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            log.info("HTTP Status: {}", response != null ? response.status() : "null");

            if (response == null || !response.ok()) {
                log.error("Failed to load page");
                return;
            }

            page.waitForTimeout(2000);

            // Analyze page structure
            log.info("\n--- Page Structure Analysis ---");

            // Check for article elements
            int articleCount = page.locator("article").count();
            log.info("Found <article> elements: {}", articleCount);

            // Check for common card selectors
            String[] selectors = {
                    "[data-testid='post-preview']",
                    "div[class*='post']",
                    "div[class*='card']",
                    "div[class*='streamItem']",
                    "a[data-action='open-post']"
            };

            for (String selector : selectors) {
                int count = page.locator(selector).count();
                if (count > 0) {
                    log.info("Found '{}': {} elements", selector, count);
                }
            }

            // Try scraping with current implementation
            log.info("\n--- Scraping Test ---");
            try {
                List<CrawledArticle> articles = scraper.scrapeArticleList(url);
                log.info("Scraped {} articles", articles.size());

                for (int i = 0; i < Math.min(articles.size(), 3); i++) {
                    CrawledArticle article = articles.get(i);
                    log.info("\nArticle {}:", i + 1);
                    log.info("  Title: {}", truncate(article.title(), 50));
                    log.info("  Author: {}", article.author());
                    log.info("  Date: {}", article.publishedAt());
                    log.info("  Link: {}", truncate(article.link(), 60));
                }

                if (articles.isEmpty()) {
                    log.warn("No articles found - may need selector adjustment");
                    analyzeFirstElements(page);
                }

            } catch (Exception e) {
                log.error("Scraping failed: {}", e.getMessage());
                analyzeFirstElements(page);
            }

        } catch (Exception e) {
            log.error("Test failed: {}", e.getMessage(), e);
        } finally {
            page.close();
            context.close();
        }
    }

    private void analyzeCustomDomain(String url) {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .setViewportSize(1920, 1080)
        );

        Page page = context.newPage();

        try {
            Response response = page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(30000)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            log.info("HTTP Status: {}", response != null ? response.status() : "null");

            if (response == null || !response.ok()) {
                log.error("Failed to load page");
                return;
            }

            page.waitForTimeout(2000);

            // Check if this is Medium or different platform
            String htmlContent = page.content();
            boolean isMedium = htmlContent.contains("medium.com") || htmlContent.contains("Medium");

            log.info("Is Medium platform: {}", isMedium);
            log.info("Page title: {}", page.title());

            // Analyze page structure
            log.info("\n--- Page Structure Analysis (Custom Domain) ---");

            // Common blog selectors
            String[] selectors = {
                    "article",
                    "[class*='post']",
                    "[class*='article']",
                    "[class*='card']",
                    "[class*='item']",
                    "[class*='entry']",
                    "li[class*='list']",
                    "div[class*='list'] > div",
                    "ul[class*='post'] > li",
                    "main article",
                    "main > div > div"
            };

            for (String selector : selectors) {
                int count = page.locator(selector).count();
                if (count > 0 && count < 50) {
                    log.info("Found '{}': {} elements", selector, count);
                }
            }

            // Look for common title patterns
            log.info("\n--- Title patterns ---");
            for (String h : List.of("h1", "h2", "h3")) {
                Locator headings = page.locator(h);
                int count = headings.count();
                if (count > 0) {
                    log.info("Found {} {} elements:", count, h);
                    for (int i = 0; i < Math.min(count, 3); i++) {
                        String text = headings.nth(i).textContent().trim();
                        if (!text.isEmpty() && text.length() < 200) {
                            log.info("  {}: {}", h, truncate(text, 60));
                        }
                    }
                }
            }

            // Look for date patterns
            log.info("\n--- Date patterns ---");
            Locator timeElements = page.locator("time");
            int timeCount = timeElements.count();
            log.info("Found {} <time> elements", timeCount);
            for (int i = 0; i < Math.min(timeCount, 3); i++) {
                String datetime = timeElements.nth(i).getAttribute("datetime");
                String text = timeElements.nth(i).textContent().trim();
                log.info("  time[{}]: datetime={}, text={}", i, datetime, text);
            }

            // Look for author patterns
            log.info("\n--- Author patterns ---");
            for (String selector : List.of("a[href*='author']", "[class*='author']", "[class*='writer']")) {
                Locator authors = page.locator(selector);
                int count = authors.count();
                if (count > 0) {
                    log.info("Found '{}': {}", selector, count);
                    for (int i = 0; i < Math.min(count, 3); i++) {
                        String text = authors.nth(i).textContent().trim();
                        if (!text.isEmpty()) {
                            log.info("  {}", truncate(text, 40));
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Test failed: {}", e.getMessage(), e);
        } finally {
            page.close();
            context.close();
        }
    }

    private void analyzeFirstElements(Page page) {
        log.info("\n--- First Article Element Analysis ---");

        Locator articles = page.locator("article");
        if (articles.count() > 0) {
            Locator first = articles.first();
            String innerText = first.innerText();
            log.info("First article innerText:\n{}", truncate(innerText, 500));
        } else {
            log.info("No <article> elements found");

            // Try finding any container with multiple items
            Locator divs = page.locator("main > div > div, section > div > div");
            log.info("Found {} potential container divs", divs.count());

            if (divs.count() > 3) {
                log.info("First container innerText:\n{}", truncate(divs.first().innerText(), 300));
            }
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        text = text.replaceAll("\\s+", " ").trim();
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
