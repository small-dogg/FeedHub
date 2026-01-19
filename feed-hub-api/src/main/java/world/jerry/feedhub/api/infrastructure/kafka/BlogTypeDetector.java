package world.jerry.feedhub.api.infrastructure.kafka;

import org.springframework.stereotype.Component;
import world.jerry.feedhub.api.domain.rss.BlogType;

import java.util.regex.Pattern;

/**
 * URL 패턴을 분석하여 블로그 플랫폼 유형을 감지
 */
@Component
public class BlogTypeDetector {

    private static final Pattern TISTORY_PATTERN = Pattern.compile(
            "https?://[\\w-]+\\.tistory\\.com/?.*", Pattern.CASE_INSENSITIVE);

    private static final Pattern MEDIUM_PATTERN = Pattern.compile(
            "https?://(medium\\.com/@[\\w-]+|[\\w-]+\\.medium\\.com)/?.*", Pattern.CASE_INSENSITIVE);

    private static final Pattern VELOG_PATTERN = Pattern.compile(
            "https?://velog\\.io/@[\\w-]+/?.*", Pattern.CASE_INSENSITIVE);

    private static final Pattern GITHUB_BLOG_PATTERN = Pattern.compile(
            "https?://[\\w-]+\\.github\\.io/?.*", Pattern.CASE_INSENSITIVE);

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
        }

        // rssUrl 검사
        if (rssUrl != null && !rssUrl.isBlank()) {
            return matchUrl(rssUrl);
        }

        return BlogType.UNKNOWN;
    }

    private BlogType matchUrl(String url) {
        if (TISTORY_PATTERN.matcher(url).matches()) {
            return BlogType.TISTORY;
        }
        if (MEDIUM_PATTERN.matcher(url).matches()) {
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
