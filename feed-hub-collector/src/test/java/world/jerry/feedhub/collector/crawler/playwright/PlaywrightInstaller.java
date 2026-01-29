package world.jerry.feedhub.collector.crawler.playwright;

import com.microsoft.playwright.CLI;

/**
 * Utility to install Playwright browsers
 * Run this main method to install Chromium browser
 */
public class PlaywrightInstaller {

    public static void main(String[] args) throws Exception {
        System.out.println("Installing Playwright Chromium browser...");

        // This will trigger Playwright CLI to install browsers
        CLI.main(new String[]{"install", "chromium"});

        System.out.println("Installation complete!");
    }
}
