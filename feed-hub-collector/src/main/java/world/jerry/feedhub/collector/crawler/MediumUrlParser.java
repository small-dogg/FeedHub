package world.jerry.feedhub.collector.crawler;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.YearMonth;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Medium URL 패턴 분석 및 크롤링 URL 생성
 *
 * Supported URL patterns:
 * - USER_PROFILE: medium.com/@username
 * - PUBLICATION_ALL: medium.com/publication-name/all
 * - PUBLICATION_ARCHIVE: medium.com/publication-name/archive/YYYY/MM
 * - CUSTOM_DOMAIN: custom.domain.com (Medium publication with custom domain)
 * - TAGGED: medium.com/tagged/topic-name
 */
@Slf4j
@Component
public class MediumUrlParser {

    private static final Pattern USER_PROFILE_PATTERN = Pattern.compile("medium\\.com/@([^/]+)");
    private static final Pattern PUBLICATION_ALL_PATTERN = Pattern.compile("medium\\.com/([^@][^/]+)/all");
    private static final Pattern PUBLICATION_ARCHIVE_PATTERN = Pattern.compile("medium\\.com/([^@][^/]+)/archive/(\\d{4})/(\\d{2})");
    private static final Pattern TAGGED_PATTERN = Pattern.compile("medium\\.com/tagged/([^/]+)");
    private static final Pattern PUBLICATION_BASE_PATTERN = Pattern.compile("medium\\.com/([^@][^/]+)/?$");

    /**
     * URL 패턴 타입
     */
    public enum PageType {
        USER_PROFILE,       // /@username
        PUBLICATION_ALL,    // /publication/all
        PUBLICATION_ARCHIVE, // /publication/archive/YYYY/MM
        CUSTOM_DOMAIN,      // techblog.example.com
        TAGGED,             // /tagged/topic
        UNKNOWN
    }

    /**
     * 파싱 결과
     */
    @Getter
    public static class ParseResult {
        private final PageType pageType;
        private final String originalUrl;
        private final String crawlUrl;
        private final String identifier; // username, publication name, or tag
        private final YearMonth archiveMonth; // for PUBLICATION_ARCHIVE

        private ParseResult(PageType pageType, String originalUrl, String crawlUrl,
                           String identifier, YearMonth archiveMonth) {
            this.pageType = pageType;
            this.originalUrl = originalUrl;
            this.crawlUrl = crawlUrl;
            this.identifier = identifier;
            this.archiveMonth = archiveMonth;
        }

        public static ParseResult of(PageType pageType, String originalUrl, String crawlUrl, String identifier) {
            return new ParseResult(pageType, originalUrl, crawlUrl, identifier, null);
        }

        public static ParseResult withArchive(String originalUrl, String crawlUrl, String identifier, YearMonth archiveMonth) {
            return new ParseResult(PageType.PUBLICATION_ARCHIVE, originalUrl, crawlUrl, identifier, archiveMonth);
        }

        public static ParseResult unknown(String originalUrl) {
            return new ParseResult(PageType.UNKNOWN, originalUrl, originalUrl, null, null);
        }
    }

    /**
     * URL을 분석하여 타입과 크롤링 대상 URL 결정
     */
    public ParseResult parse(String url) {
        if (url == null || url.isBlank()) {
            return ParseResult.unknown(url);
        }

        String normalizedUrl = normalizeUrl(url);

        // Check for user profile: /@username
        Matcher userMatcher = USER_PROFILE_PATTERN.matcher(normalizedUrl);
        if (userMatcher.find()) {
            String username = userMatcher.group(1);
            return ParseResult.of(PageType.USER_PROFILE, normalizedUrl, normalizedUrl, username);
        }

        // Check for publication/all: /publication-name/all
        Matcher pubAllMatcher = PUBLICATION_ALL_PATTERN.matcher(normalizedUrl);
        if (pubAllMatcher.find()) {
            String publication = pubAllMatcher.group(1);
            return ParseResult.of(PageType.PUBLICATION_ALL, normalizedUrl, normalizedUrl, publication);
        }

        // Check for publication/archive: /publication-name/archive/YYYY/MM
        Matcher archiveMatcher = PUBLICATION_ARCHIVE_PATTERN.matcher(normalizedUrl);
        if (archiveMatcher.find()) {
            String publication = archiveMatcher.group(1);
            int year = Integer.parseInt(archiveMatcher.group(2));
            int month = Integer.parseInt(archiveMatcher.group(3));
            YearMonth ym = YearMonth.of(year, month);
            return ParseResult.withArchive(normalizedUrl, normalizedUrl, publication, ym);
        }

        // Check for tagged: /tagged/topic
        Matcher taggedMatcher = TAGGED_PATTERN.matcher(normalizedUrl);
        if (taggedMatcher.find()) {
            String tag = taggedMatcher.group(1);
            return ParseResult.of(PageType.TAGGED, normalizedUrl, normalizedUrl, tag);
        }

        // Check for publication base URL (redirect to /all)
        Matcher pubBaseMatcher = PUBLICATION_BASE_PATTERN.matcher(normalizedUrl);
        if (pubBaseMatcher.find()) {
            String publication = pubBaseMatcher.group(1);
            String allUrl = "https://medium.com/" + publication + "/all";
            return ParseResult.of(PageType.PUBLICATION_ALL, normalizedUrl, allUrl, publication);
        }

        // Check for custom domain (not medium.com)
        if (!normalizedUrl.contains("medium.com")) {
            String host = extractHost(normalizedUrl);
            // Custom domain - default to /all page for publications
            String crawlUrl = normalizedUrl.endsWith("/all") ? normalizedUrl : normalizedUrl + "/all";
            return ParseResult.of(PageType.CUSTOM_DOMAIN, normalizedUrl, crawlUrl, host);
        }

        return ParseResult.unknown(normalizedUrl);
    }

    /**
     * 지정된 crawlUrl이 있으면 분석, 없으면 siteUrl/rssUrl에서 추론
     */
    public ParseResult parseWithFallback(String crawlUrl, String siteUrl, String rssUrl) {
        // crawlUrl이 명시적으로 지정된 경우 우선
        if (crawlUrl != null && !crawlUrl.isBlank()) {
            return parse(crawlUrl);
        }

        // siteUrl로 시도
        if (siteUrl != null && !siteUrl.isBlank()) {
            return parse(siteUrl);
        }

        // rssUrl에서 추출
        if (rssUrl != null && !rssUrl.isBlank()) {
            String derivedUrl = extractSiteFromRss(rssUrl);
            return parse(derivedUrl);
        }

        return ParseResult.unknown(null);
    }

    /**
     * 다음 아카이브 페이지 URL 생성 (페이지네이션용)
     */
    public String buildArchiveUrl(String basePublicationUrl, YearMonth yearMonth) {
        String base = basePublicationUrl.replaceAll("/all$", "").replaceAll("/archive/\\d{4}/\\d{2}$", "");
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        return base + "archive/" + yearMonth.getYear() + "/" + String.format("%02d", yearMonth.getMonthValue());
    }

    /**
     * RSS URL에서 사이트 URL 추출
     */
    private String extractSiteFromRss(String rssUrl) {
        // https://medium.com/feed/@username -> https://medium.com/@username
        // https://medium.com/feed/publication -> https://medium.com/publication
        return rssUrl.replace("/feed/", "/").replace("/feed", "");
    }

    private String normalizeUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        // Remove trailing slash for consistency
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String extractHost(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getHost();
        } catch (Exception e) {
            return url;
        }
    }
}
