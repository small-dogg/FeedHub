package world.jerry.feedhub.collector.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Playwright Browser configuration for web scraping
 * - Provides Chromium browser instance
 * - Configured to bypass bot detection
 */
@Slf4j
@Configuration
public class PlaywrightConfig {

    /**
     * Playwright instance (global)
     */
    @Bean(destroyMethod = "close")
    public Playwright playwright() {
        log.info("Initializing Playwright instance");
        return Playwright.create();
    }

    /**
     * Chromium Browser instance with anti-detection settings
     * - Headless mode for performance
     * - Disables automation detection flags
     */
    @Bean(destroyMethod = "close")
    public Browser browser(Playwright playwright) {
        log.info("Launching Chromium browser with anti-detection settings");

        return playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                // Disable automation detection (critical for bypassing bot detection)
                .setArgs(Arrays.asList(
                        "--disable-blink-features=AutomationControlled",
                        "--disable-dev-shm-usage",
                        "--no-sandbox"
                ))
        );
    }
}
