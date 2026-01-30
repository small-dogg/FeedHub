package world.jerry.feedhub.collector.crawler.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import world.jerry.feedhub.collector.domain.CrawledArticle;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Playwright-based Medium scraper for bypassing bot detection
 * - Uses Chromium headless browser
 * - Handles JavaScript rendering
 * - Extracts article content from Medium pages
 */
@Slf4j
@Component
public class PlaywrightMediumScraper {

    private final Browser browser;

    private static final int PAGE_LOAD_TIMEOUT_MS = 30000;
    private static final int SELECTOR_WAIT_TIMEOUT_MS = 10000;
    private static final int DESCRIPTION_MAX_LENGTH = 500;

    // Pagination settings
    private static final int DEFAULT_MAX_ARTICLES = 50;
    private static final int SCROLL_WAIT_MS = 2000;
    private static final int MAX_SCROLL_ATTEMPTS = 20;

    // User agent to mimic real browser
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    public PlaywrightMediumScraper(Browser browser) {
        this.browser = browser;
    }

    /**
     * Scrape a single Medium article
     *
     * @param articleUrl Full article URL
     * @return Crawled article data
     */
    public CrawledArticle scrape(String articleUrl) {
        log.debug("Starting scrape for URL: {}", articleUrl);

        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent(USER_AGENT)
                // Set realistic viewport
                .setViewportSize(1920, 1080)
        );

        Page page = context.newPage();

        try {
            // Navigate to article page
            // Use DOMCONTENTLOADED instead of NETWORKIDLE for Medium pages
            // (Medium has continuous network activity that prevents NETWORKIDLE)
            Response response = page.navigate(articleUrl, new Page.NavigateOptions()
                    .setTimeout(PAGE_LOAD_TIMEOUT_MS)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            if (response == null || !response.ok()) {
                int status = response != null ? response.status() : 0;
                log.error("Failed to load page: {} (HTTP {})", articleUrl, status);
                throw new RuntimeException("Failed to load page: HTTP " + status);
            }

            log.debug("Page loaded successfully: {}", articleUrl);

            // Wait for article content to be rendered
            // Reduced timeout for faster scraping (Medium pages load quickly)
            try {
                page.waitForSelector("article h1, h1, h2", new Page.WaitForSelectorOptions()
                        .setTimeout(5000) // 5 seconds is enough
                );
            } catch (TimeoutError e) {
                log.debug("Article title selector not found within timeout: {}", articleUrl);
                // Continue anyway, try to parse what's available
            }

            // Parse article content
            return parseArticlePage(page, articleUrl);

        } catch (Exception e) {
            log.error("Failed to scrape article: {} - {}", articleUrl, e.getMessage(), e);
            throw new RuntimeException("Scraping failed: " + e.getMessage(), e);
        } finally {
            page.close();
            context.close();
        }
    }

    /**
     * Scrape article list from Medium blog/publication page (default max articles)
     *
     * @param listPageUrl URL of the list page (e.g., blog.medium.com, medium.com/@user)
     * @return List of crawled articles with summary info
     */
    public List<CrawledArticle> scrapeArticleList(String listPageUrl) {
        return scrapeArticleList(listPageUrl, DEFAULT_MAX_ARTICLES);
    }

    /**
     * Scrape article list from Medium blog/publication page with infinite scroll support
     * - Extracts article cards with title, summary, author, date, link
     * - Handles Medium's virtualized scrolling by collecting articles incrementally
     *
     * @param listPageUrl URL of the list page (e.g., blog.medium.com, medium.com/@user)
     * @param maxArticles Maximum number of articles to collect
     * @return List of crawled articles with summary info
     */
    public List<CrawledArticle> scrapeArticleList(String listPageUrl, int maxArticles) {
        log.info("Scraping article list from: {} (max: {})", listPageUrl, maxArticles);

        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent(USER_AGENT)
                .setViewportSize(1920, 1080)
        );

        Page page = context.newPage();
        List<CrawledArticle> articles = new ArrayList<>();
        java.util.Set<String> collectedLinks = new java.util.HashSet<>();

        try {
            Response response = page.navigate(listPageUrl, new Page.NavigateOptions()
                    .setTimeout(PAGE_LOAD_TIMEOUT_MS)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            if (response == null || !response.ok()) {
                int status = response != null ? response.status() : 0;
                log.error("Failed to load list page: {} (HTTP {})", listPageUrl, status);
                throw new RuntimeException("Failed to load page: HTTP " + status);
            }

            // Wait for initial content to render
            page.waitForTimeout(2000);

            // First, collect initial visible articles
            Locator articleCards = page.locator("article");
            int visibleCount = articleCards.count();
            log.debug("Initial load: {} visible cards", visibleCount);

            for (int i = 0; i < visibleCount && articles.size() < maxArticles; i++) {
                try {
                    Locator card = articleCards.nth(i);
                    CrawledArticle article = parseArticleCard(card, listPageUrl);

                    if (article != null && article.title() != null && !article.title().isEmpty()) {
                        String link = article.link();
                        if (link != null && !collectedLinks.contains(link)) {
                            collectedLinks.add(link);
                            articles.add(article);
                            log.debug("Collected article {}: {}", articles.size(), truncateForLog(article.title(), 40));
                        }
                    }
                } catch (Exception e) {
                    log.trace("Failed to parse card {}: {}", i, e.getMessage());
                }
            }

            // Scroll to load more articles
            // Medium resets DOM ~2 seconds after scroll, so we scroll and collect immediately
            int scrollAttempts = 0;
            int noNewArticlesCount = 0;
            int lastScrollTarget = visibleCount - 1;

            while (articles.size() < maxArticles && scrollAttempts < MAX_SCROLL_ATTEMPTS) {
                int previousSize = articles.size();

                // Scroll to the next batch and immediately collect
                int scrollTarget = lastScrollTarget + 5; // Scroll 5 articles at a time
                List<CrawledArticle> newArticles = scrollAndCollect(page, listPageUrl, collectedLinks, scrollTarget);

                for (CrawledArticle article : newArticles) {
                    if (articles.size() >= maxArticles) break;
                    articles.add(article);
                    log.debug("Collected article {}: {}", articles.size(), truncateForLog(article.title(), 40));
                }

                lastScrollTarget = scrollTarget;
                scrollAttempts++;

                log.debug("Scroll {}: collected {} new articles (total: {})",
                        scrollAttempts, newArticles.size(), articles.size());

                // Check if we've collected enough
                if (articles.size() >= maxArticles) {
                    log.info("Reached target article count: {}", articles.size());
                    break;
                }

                // Check if we found new articles
                if (articles.size() == previousSize) {
                    noNewArticlesCount++;
                    if (noNewArticlesCount >= 3) {
                        log.info("No new articles after {} scroll attempts, stopping", noNewArticlesCount);
                        break;
                    }
                } else {
                    noNewArticlesCount = 0;
                }

                // Wait before next scroll attempt (page might need time to stabilize)
                page.waitForTimeout(SCROLL_WAIT_MS);
            }

            log.info("Successfully extracted {} articles from list page (scrolled {} times)",
                    articles.size(), scrollAttempts);
            return articles;

        } catch (Exception e) {
            log.error("Failed to scrape article list: {} - {}", listPageUrl, e.getMessage(), e);
            throw new RuntimeException("List scraping failed: " + e.getMessage(), e);
        } finally {
            page.close();
            context.close();
        }
    }

    /**
     * Scroll to load more articles and immediately collect them
     * - Medium resets DOM ~2 seconds after scroll, so we must collect immediately
     * - Scrolls to end of page to trigger infinite scroll loading
     * - Returns list of new articles found after scroll
     */
    private List<CrawledArticle> scrollAndCollect(Page page, String baseUrl,
            java.util.Set<String> collectedLinks, int targetIndex) {
        List<CrawledArticle> newArticles = new ArrayList<>();

        // Scroll to the end of current content to trigger infinite scroll
        page.evaluate("""
            (function() {
                // Scroll to the last article
                const articles = document.querySelectorAll('article');
                if (articles.length > 0) {
                    const lastArticle = articles[articles.length - 1];
                    lastArticle.scrollIntoView({ behavior: 'instant', block: 'end' });
                }
                // Also scroll a bit more past the last article
                window.scrollBy(0, 200);
            })()
            """);

        // Wait a bit for new content to load (but not too long before DOM resets)
        page.waitForTimeout(800);

        // Immediately collect all visible articles
        Locator articleCards = page.locator("article");
        int visibleCount = articleCards.count();

        for (int i = 0; i < visibleCount; i++) {
            try {
                Locator card = articleCards.nth(i);
                CrawledArticle article = parseArticleCard(card, baseUrl);

                if (article != null && article.title() != null && !article.title().isEmpty()) {
                    String link = article.link();
                    if (link != null && !collectedLinks.contains(link)) {
                        collectedLinks.add(link);
                        newArticles.add(article);
                    }
                }
            } catch (Exception e) {
                log.trace("Failed to parse card during scroll: {}", e.getMessage());
            }
        }

        return newArticles;
    }

    /**
     * Scrape articles using Archive-based pagination
     * - Medium archive pages: /publication/archive/YYYY/MM
     * - More reliable than infinite scroll (which Medium blocks)
     * - Iterates through monthly archive pages
     *
     * @param publicationUrl Base publication URL (e.g., https://medium.com/daangn)
     * @param monthsBack Number of months to go back from current month
     * @param maxArticles Maximum total articles to collect
     * @return List of crawled articles
     */
    public List<CrawledArticle> scrapeWithArchivePagination(String publicationUrl, int monthsBack, int maxArticles) {
        log.info("Scraping with archive pagination: {} (months: {}, max: {})", publicationUrl, monthsBack, maxArticles);

        List<CrawledArticle> allArticles = new ArrayList<>();
        java.util.Set<String> collectedLinks = new java.util.HashSet<>();

        // Extract publication name from URL
        String baseUrl = publicationUrl.replaceAll("/all$", "").replaceAll("/$", "");

        YearMonth currentMonth = YearMonth.now();

        for (int i = 0; i <= monthsBack && allArticles.size() < maxArticles; i++) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            String archiveUrl = String.format("%s/archive/%d/%02d",
                    baseUrl, targetMonth.getYear(), targetMonth.getMonthValue());

            log.debug("Fetching archive page: {}", archiveUrl);

            try {
                List<CrawledArticle> monthArticles = scrapeArchivePage(archiveUrl, collectedLinks);

                for (CrawledArticle article : monthArticles) {
                    if (allArticles.size() >= maxArticles) break;
                    if (article.link() != null && !collectedLinks.contains(article.link())) {
                        collectedLinks.add(article.link());
                        allArticles.add(article);
                    }
                }

                log.debug("Archive {}: found {} articles (total: {})",
                        archiveUrl, monthArticles.size(), allArticles.size());

                // Rate limiting between archive pages
                if (i < monthsBack) {
                    Thread.sleep(1000 + (long) (Math.random() * 1000));
                }

            } catch (Exception e) {
                log.warn("Failed to fetch archive {}: {}", archiveUrl, e.getMessage());
                // Continue with next month
            }
        }

        log.info("Archive pagination complete: {} articles from {} months", allArticles.size(), monthsBack + 1);
        return allArticles;
    }

    /**
     * Scrape a single archive page
     */
    private List<CrawledArticle> scrapeArchivePage(String archiveUrl, java.util.Set<String> existingLinks) {
        List<CrawledArticle> articles = new ArrayList<>();

        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent(USER_AGENT)
                .setViewportSize(1920, 1080)
        );

        Page page = context.newPage();

        try {
            Response response = page.navigate(archiveUrl, new Page.NavigateOptions()
                    .setTimeout(PAGE_LOAD_TIMEOUT_MS)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            if (response == null || !response.ok()) {
                log.debug("Archive page not found or error: {} (HTTP {})",
                        archiveUrl, response != null ? response.status() : 0);
                return articles;
            }

            // Wait for content
            page.waitForTimeout(2000);

            // Parse all visible articles (no scrolling - archive pages show all)
            Locator articleCards = page.locator("article");
            int cardCount = articleCards.count();

            for (int i = 0; i < cardCount; i++) {
                try {
                    Locator card = articleCards.nth(i);
                    CrawledArticle article = parseArticleCard(card, archiveUrl);

                    if (article != null && article.title() != null && !article.title().isEmpty()) {
                        String link = article.link();
                        if (link != null && !existingLinks.contains(link)) {
                            articles.add(article);
                        }
                    }
                } catch (Exception e) {
                    log.trace("Failed to parse card {}: {}", i, e.getMessage());
                }
            }

        } finally {
            page.close();
            context.close();
        }

        return articles;
    }


    /**
     * Truncate text for logging (to avoid flooding logs)
     */
    private String truncateForLog(String text, int maxLength) {
        if (text == null) return "null";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    /**
     * Parse article card from list page
     * - Extracts: title, summary, author, date, link
     */
    private CrawledArticle parseArticleCard(Locator card, String baseUrl) {
        // Extract title from h2
        String title = extractFromCard(card, "h2");
        if (title == null || title.isEmpty()) {
            return null; // Skip cards without title
        }

        // Extract summary from h3 (subtitle/preview)
        String summary = extractFromCard(card, "h3");

        // Extract author from link with /@
        String author = extractAuthorFromCard(card);

        // Fallback: extract author from baseUrl if it's a user profile page
        if (author == null && baseUrl.contains("/@")) {
            author = extractAuthorFromUrl(baseUrl);
        }

        // Extract date from span (e.g., "Jan 15")
        Instant publishedAt = extractDateFromCard(card);

        // Extract article link
        String link = extractLinkFromCard(card, baseUrl);

        return CrawledArticle.builder()
                .title(title)
                .link(link)
                .description(summary)
                .author(author)
                .publishedAt(publishedAt)
                .build();
    }

    /**
     * Extract author username from URL (for user profile pages)
     * e.g., https://medium.com/@springrod -> springrod
     */
    private String extractAuthorFromUrl(String url) {
        try {
            int atIndex = url.indexOf("/@");
            if (atIndex != -1) {
                int start = atIndex + 2;
                int end = url.indexOf("/", start);
                if (end == -1) {
                    end = url.indexOf("?", start);
                }
                if (end == -1) {
                    end = url.length();
                }
                String username = url.substring(start, end);
                return username.isEmpty() ? null : username;
            }
        } catch (Exception e) {
            log.trace("Failed to extract author from URL: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extract text from card using selector
     */
    private String extractFromCard(Locator card, String selector) {
        try {
            Locator element = card.locator(selector).first();
            if (element.count() > 0) {
                String text = element.textContent().trim();
                return text.isEmpty() ? null : text;
            }
        } catch (Exception e) {
            log.trace("Failed to extract {} from card: {}", selector, e.getMessage());
        }
        return null;
    }

    /**
     * Extract author from article card
     * - Looks for links containing /@username
     * - Filters out long text and UI elements (like clap icons)
     */
    private String extractAuthorFromCard(Locator card) {
        try {
            // Look for author link (href contains /@)
            Locator authorLinks = card.locator("a[href*='/@']");
            int count = authorLinks.count();

            for (int i = 0; i < count; i++) {
                Locator link = authorLinks.nth(i);
                String text = link.textContent().trim();

                // Author name validation:
                // - Should be short (< 40 chars)
                // - Should not contain article content or UI elements
                // - Should look like a name (mostly letters and spaces)
                if (!text.isEmpty() &&
                        text.length() < 40 &&
                        !text.contains("min read") &&
                        !text.contains("\n") &&
                        !text.contains("icon") &&       // Filter out UI text
                        !text.contains("clap") &&       // Filter out clap button
                        !text.contains("response") &&   // Filter out response count
                        !text.matches(".*\\d{2,}.*") && // Filter out text with multiple digits
                        text.matches(".*[a-zA-Z].*")) { // Must contain at least one letter
                    return text;
                }
            }

            // Fallback: extract author from href pattern (/@username)
            // This is more reliable for user profile pages
            for (int i = 0; i < count; i++) {
                String href = authorLinks.nth(i).getAttribute("href");
                if (href != null && href.contains("/@")) {
                    // Extract username from href: /@username/article or /@username?...
                    int start = href.indexOf("/@") + 2;
                    int end = href.indexOf("/", start);
                    if (end == -1) {
                        end = href.indexOf("?", start);
                    }
                    if (end == -1) {
                        end = href.length();
                    }
                    String username = href.substring(start, end);

                    // Validate username
                    if (!username.isEmpty() &&
                            username.length() < 50 &&
                            !username.contains("-")) {  // Usernames don't have hyphens (article slugs do)
                        return username;
                    }
                }
            }
        } catch (Exception e) {
            log.trace("Failed to extract author from card: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extract date from article card
     * - Looks for span with date pattern (e.g., "Jan 15", "Dec 31")
     */
    private Instant extractDateFromCard(Locator card) {
        // Pattern for Medium date format: "Jan 15", "Dec 31", etc.
        Pattern datePattern = Pattern.compile("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+(\\d{1,2})");

        try {
            Locator spans = card.locator("span");
            int count = spans.count();

            for (int i = 0; i < count; i++) {
                String text = spans.nth(i).textContent().trim();
                Matcher matcher = datePattern.matcher(text);

                if (matcher.find()) {
                    String month = matcher.group(1);
                    int day = Integer.parseInt(matcher.group(2));

                    // Assume current year (Medium shows just month and day for recent posts)
                    int year = LocalDate.now().getYear();
                    String dateStr = String.format("%s %d, %d", month, day, year);

                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
                        LocalDate date = LocalDate.parse(dateStr, formatter);

                        // If parsed date is in future, it's from last year
                        if (date.isAfter(LocalDate.now())) {
                            date = date.minusYears(1);
                        }

                        return date.atStartOfDay().toInstant(ZoneOffset.UTC);
                    } catch (Exception e) {
                        log.trace("Failed to parse date: {}", dateStr);
                    }
                }
            }
        } catch (Exception e) {
            log.trace("Failed to extract date from card: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extract article link from card
     * - Returns full URL
     * - Handles both publication pages and user profile pages
     */
    private String extractLinkFromCard(Locator card, String baseUrl) {
        try {
            // Look for link to article
            Locator links = card.locator("a[href]");
            int count = links.count();

            for (int i = 0; i < count; i++) {
                String href = links.nth(i).getAttribute("href");
                if (href == null || href.equals("#")) {
                    continue;
                }

                // Skip unwanted link patterns
                if (href.contains("/m/signin") ||     // Signin redirects
                        href.contains("/tag/")) {          // Tag links
                    continue;
                }

                // Check if it's an article link
                // Article links contain a slug with hyphens and/or a hash ID
                boolean isArticleLink = href.matches(".*-[a-f0-9]{8,}.*") ||  // slug-hashid pattern
                        (href.contains("-") && href.contains("?source="));     // slug with source param

                // For user profile pages: /@username/article-slug is the article link
                // For publication pages: /publication-name/article-slug (no /@)
                if (isArticleLink) {
                    // Convert relative URL to absolute
                    if (href.startsWith("/")) {
                        String baseDomain = extractBaseDomain(baseUrl);
                        return baseDomain + href;
                    }
                    // Handle full URLs
                    if (href.startsWith("http")) {
                        return href;
                    }
                }
            }

            // Fallback: look for the first link containing title text
            Locator firstLink = card.locator("a[href*='-']").first();
            if (firstLink.count() > 0) {
                String href = firstLink.getAttribute("href");
                if (href != null && !href.contains("/m/signin") && !href.contains("/tag/")) {
                    if (href.startsWith("/")) {
                        return extractBaseDomain(baseUrl) + href;
                    }
                    return href;
                }
            }
        } catch (Exception e) {
            log.trace("Failed to extract link from card: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extract base domain from URL
     */
    private String extractBaseDomain(String url) {
        try {
            java.net.URL parsed = new java.net.URL(url);
            return parsed.getProtocol() + "://" + parsed.getHost();
        } catch (Exception e) {
            return "https://medium.com";
        }
    }

    /**
     * Scrape multiple Medium articles
     *
     * @param articleUrls List of article URLs
     * @return List of crawled articles
     */
    public List<CrawledArticle> scrapeMultiple(List<String> articleUrls) {
        log.info("Starting scrape for {} articles", articleUrls.size());

        List<CrawledArticle> articles = new ArrayList<>();

        for (int i = 0; i < articleUrls.size(); i++) {
            String url = articleUrls.get(i);
            try {
                CrawledArticle article = scrape(url);
                articles.add(article);
                log.info("Successfully scraped article {}/{}: {}", i + 1, articleUrls.size(), url);

                // Rate limiting: wait between requests
                if (i < articleUrls.size() - 1) {
                    Thread.sleep(3000 + (long) (Math.random() * 2000)); // 3-5 seconds
                }

            } catch (Exception e) {
                log.error("Failed to scrape article {}/{}: {} - {}",
                        i + 1, articleUrls.size(), url, e.getMessage());
                // Continue with next article
            }
        }

        log.info("Scraping completed: {}/{} articles successful",
                articles.size(), articleUrls.size());

        return articles;
    }

    /**
     * Parse article content from Playwright Page
     */
    private CrawledArticle parseArticlePage(Page page, String url) {
        String title = extractTitle(page);
        String content = extractContent(page);
        String author = extractAuthor(page);
        Instant publishedAt = extractPublishedDate(page);

        return CrawledArticle.builder()
                .title(title)
                .link(url)
                .description(truncate(content, DESCRIPTION_MAX_LENGTH))
                .author(author)
                .publishedAt(publishedAt)
                .build();
    }

    /**
     * Extract article title
     * - Primary: article h1
     * - Fallback: og:title meta tag
     */
    private String extractTitle(Page page) {
        try {
            // Try article h1 first
            Locator titleLocator = page.locator("article h1").first();
            if (titleLocator.count() > 0) {
                String title = titleLocator.textContent().trim();
                if (!title.isEmpty()) {
                    return title;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract title from article h1: {}", e.getMessage());
        }

        try {
            // Fallback: og:title meta tag
            String ogTitle = page.getAttribute("meta[property='og:title']", "content");
            if (ogTitle != null && !ogTitle.isEmpty()) {
                return ogTitle.trim();
            }
        } catch (Exception e) {
            log.debug("Failed to extract title from og:title: {}", e.getMessage());
        }

        log.warn("Could not extract title, using default");
        return "Untitled";
    }

    /**
     * Extract article content (full text)
     * - Extracts all paragraphs from article body
     * - Joins with newlines
     */
    private String extractContent(Page page) {
        StringBuilder content = new StringBuilder();

        try {
            // Try article section paragraphs
            Locator paragraphs = page.locator("article section p, article div p");
            int count = paragraphs.count();

            for (int i = 0; i < count; i++) {
                String text = paragraphs.nth(i).textContent().trim();
                if (!text.isEmpty()) {
                    content.append(text).append("\n\n");
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract content: {}", e.getMessage());
        }

        String result = content.toString().trim();
        if (result.isEmpty()) {
            log.warn("No content extracted from article");
            return "";
        }

        return result;
    }

    /**
     * Extract article author
     * - Primary: data-testid='authorName'
     * - Fallback: author meta tag
     */
    private String extractAuthor(Page page) {
        try {
            // Try authorName first
            Locator authorLocator = page.locator("[data-testid='authorName']").first();
            if (authorLocator.count() > 0) {
                String author = authorLocator.textContent().trim();
                if (!author.isEmpty()) {
                    return author;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract author from data-testid: {}", e.getMessage());
        }

        try {
            // Fallback: author meta tag
            String metaAuthor = page.getAttribute("meta[property='author']", "content");
            if (metaAuthor != null && !metaAuthor.isEmpty()) {
                return metaAuthor.trim();
            }
        } catch (Exception e) {
            log.debug("Failed to extract author from meta tag: {}", e.getMessage());
        }

        // Try alternative selectors
        try {
            Locator authorLink = page.locator("a[rel='author']").first();
            if (authorLink.count() > 0) {
                String author = authorLink.textContent().trim();
                if (!author.isEmpty()) {
                    return author;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract author from rel=author link: {}", e.getMessage());
        }

        return null; // Author is optional
    }

    /**
     * Extract published date
     * - Primary: time[datetime] attribute
     * - Fallback: article:published_time meta tag
     */
    private Instant extractPublishedDate(Page page) {
        try {
            // Try time[datetime] first
            String datetime = page.getAttribute("time[datetime]", "datetime");
            if (datetime != null && !datetime.isEmpty()) {
                return parseDateTime(datetime);
            }
        } catch (Exception e) {
            log.debug("Failed to extract datetime from time element: {}", e.getMessage());
        }

        try {
            // Fallback: article:published_time meta tag
            String metaTime = page.getAttribute("meta[property='article:published_time']", "content");
            if (metaTime != null && !metaTime.isEmpty()) {
                return parseDateTime(metaTime);
            }
        } catch (Exception e) {
            log.debug("Failed to extract datetime from meta tag: {}", e.getMessage());
        }

        log.warn("Could not extract published date");
        return null; // Published date is optional
    }

    /**
     * Parse datetime string to Instant
     */
    private Instant parseDateTime(String datetime) {
        try {
            return Instant.parse(datetime);
        } catch (DateTimeParseException e) {
            try {
                // Try ISO date format
                return DateTimeFormatter.ISO_DATE_TIME.parse(datetime, Instant::from);
            } catch (Exception ex) {
                log.warn("Failed to parse datetime: {}", datetime);
                return null;
            }
        }
    }

    /**
     * Truncate text to specified length
     */
    private String truncate(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength) + "...";
    }
}
