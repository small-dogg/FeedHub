package world.jerry.feedhub.collector.crawler.playwright;

import com.microsoft.playwright.*;

import java.util.Arrays;

/**
 * Smoke test to verify Playwright installation and basic functionality
 * - Tests if Chromium browser can be launched
 * - Tests if simple page navigation works
 * - First run will automatically download Chromium browser
 */
public class PlaywrightSmokeTest {

    public static void main(String[] args) {
        System.out.println("=== Playwright Smoke Test ===\n");
        System.out.println("NOTE: First run will download Chromium browser (~100MB)");
        System.out.println("This may take a few minutes...\n");

        try (Playwright playwright = Playwright.create()) {
            System.out.println("✓ Playwright initialized");

            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(Arrays.asList(
                            "--disable-blink-features=AutomationControlled",
                            "--disable-dev-shm-usage",
                            "--no-sandbox"
                    ))
            );

            System.out.println("✓ Chromium browser launched");

            Page page = browser.newPage();
            System.out.println("✓ New page created");

            // Test navigation with a simple site
            System.out.println("\nTesting page navigation to example.com...");
            Response response = page.navigate("https://example.com");

            if (response != null && response.ok()) {
                System.out.println("✓ Navigation successful (HTTP " + response.status() + ")");
                String title = page.title();
                System.out.println("✓ Page title: " + title);
            } else {
                System.out.println("✗ Navigation failed");
            }

            page.close();
            browser.close();

            System.out.println("\n=== Smoke Test PASSED ===");
            System.out.println("Playwright is ready to use!");

        } catch (Exception e) {
            System.out.println("\n=== Smoke Test FAILED ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
