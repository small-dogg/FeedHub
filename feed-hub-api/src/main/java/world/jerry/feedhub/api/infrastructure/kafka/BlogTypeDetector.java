package world.jerry.feedhub.api.infrastructure.kafka;

import org.springframework.stereotype.Component;
import world.jerry.feedhub.common.domain.BlogType;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * URL 패턴을 분석하여 블로그 플랫폼 유형을 감지
 */
@Component
public class BlogTypeDetector {

    private static final Pattern TISTORY_PATTERN = Pattern.compile(
            "https?://[\\w-]+\\.tistory\\.com/?.*", Pattern.CASE_INSENSITIVE);

    // Medium 유저 프로필: medium.com/@username
    private static final Pattern MEDIUM_USER_PATTERN = Pattern.compile(
            "https?://medium\\.com/@[\\w.-]+/?.*", Pattern.CASE_INSENSITIVE);

    // Medium 서브도메인: username.medium.com
    private static final Pattern MEDIUM_SUBDOMAIN_PATTERN = Pattern.compile(
            "https?://[\\w-]+\\.medium\\.com/?.*", Pattern.CASE_INSENSITIVE);

    // Medium 퍼블리케이션: medium.com/publication-name (@ 없음)
    private static final Pattern MEDIUM_PUBLICATION_PATTERN = Pattern.compile(
            "https?://medium\\.com/(?!feed|tag|search|about|creators|membership|me)[\\w-]+(?:/.*)?", Pattern.CASE_INSENSITIVE);

    // Medium RSS 피드: medium.com/feed/...
    private static final Pattern MEDIUM_RSS_PATTERN = Pattern.compile(
            "https?://medium\\.com/feed/.*", Pattern.CASE_INSENSITIVE);

    private static final Pattern VELOG_PATTERN = Pattern.compile(
            "https?://velog\\.io/@[\\w-]+/?.*", Pattern.CASE_INSENSITIVE);

    private static final Pattern GITHUB_BLOG_PATTERN = Pattern.compile(
            "https?://[\\w-]+\\.github\\.io/?.*", Pattern.CASE_INSENSITIVE);

    // 알려진 Medium 커스텀 도메인 목록
    private static final Set<String> KNOWN_MEDIUM_CUSTOM_DOMAINS = Set.of(
            "techblog.yogiyo.co.kr",
            "tech.kakao.com",
            "engineering.linecorp.com",
            "hyperconnect.github.io",
            "blog.banksalad.com"
    );

    /**
     * siteUrl과 rssUrl을 분석하여 블로그 유형 감지
     * siteUrl 우선, 없으면 rssUrl 사용
     */
    public BlogType detect(String siteUrl, String rssUrl) {
        // siteUrl 우선 검사
        if (siteUrl != null && !siteUrl.isBlank()) {
            BlogType type = matchUrl(siteUrl);
            if (type != BlogType.UNKNOWN) {
                return type;
            }

            // 알려진 Medium 커스텀 도메인 체크
            if (isKnownMediumDomain(siteUrl)) {
                return BlogType.MEDIUM;
            }
        }

        // rssUrl 검사
        if (rssUrl != null && !rssUrl.isBlank()) {
            BlogType type = matchUrl(rssUrl);
            if (type != BlogType.UNKNOWN) {
                return type;
            }

            // RSS URL이 Medium 피드 형식이면 커스텀 도메인도 Medium으로 처리
            if (isMediumRssFeed(rssUrl)) {
                return BlogType.MEDIUM;
            }
        }

        return BlogType.UNKNOWN;
    }

    private BlogType matchUrl(String url) {
        if (TISTORY_PATTERN.matcher(url).matches()) {
            return BlogType.TISTORY;
        }
        if (MEDIUM_USER_PATTERN.matcher(url).matches()) {
            return BlogType.MEDIUM;
        }
        if (MEDIUM_SUBDOMAIN_PATTERN.matcher(url).matches()) {
            return BlogType.MEDIUM;
        }
        if (MEDIUM_PUBLICATION_PATTERN.matcher(url).matches()) {
            return BlogType.MEDIUM;
        }
        if (MEDIUM_RSS_PATTERN.matcher(url).matches()) {
            return BlogType.MEDIUM;
        }
        if (VELOG_PATTERN.matcher(url).matches()) {
            return BlogType.VELOG;
        }
        if (GITHUB_BLOG_PATTERN.matcher(url).matches()) {
            return BlogType.GITHUB_BLOG;
        }
        return BlogType.UNKNOWN;
    }

    /**
     * 알려진 Medium 커스텀 도메인인지 확인
     */
    private boolean isKnownMediumDomain(String url) {
        if (url == null) return false;
        String lowerUrl = url.toLowerCase();
        return KNOWN_MEDIUM_CUSTOM_DOMAINS.stream()
                .anyMatch(lowerUrl::contains);
    }

    /**
     * Medium RSS 피드 URL인지 확인 (커스텀 도메인 포함)
     * 예: https://medium.com/feed/@username, https://custom.domain/feed
     */
    private boolean isMediumRssFeed(String rssUrl) {
        if (rssUrl == null) return false;
        // medium.com/feed 패턴
        if (MEDIUM_RSS_PATTERN.matcher(rssUrl).matches()) {
            return true;
        }
        // 커스텀 도메인의 /feed 경로 (알려진 도메인만)
        String lowerUrl = rssUrl.toLowerCase();
        return KNOWN_MEDIUM_CUSTOM_DOMAINS.stream()
                .anyMatch(domain -> lowerUrl.contains(domain) && lowerUrl.contains("/feed"));
    }

    /**
     * 블로그 유형에 해당하는 Kafka 토픽명 반환
     */
    public String getTopicName(BlogType blogType) {
        return switch (blogType) {
            case TISTORY -> "tistory-crawl";
            case MEDIUM -> "medium-crawl";
            case VELOG -> "velog-crawl";
            case GITHUB_BLOG -> "github-blog-crawl";
            case UNKNOWN -> null; // UNKNOWN은 크롤링 대상이 아님
        };
    }
}
