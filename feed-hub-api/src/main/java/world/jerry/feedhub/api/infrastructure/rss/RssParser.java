package world.jerry.feedhub.api.infrastructure.rss;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * RSS/Atom 피드 파서
 * RSS 1.0, RSS 2.0, Atom 포맷 지원
 */
@Slf4j
@Component
public class RssParser {

    /**
     * RSS URL에서 피드 엔트리를 파싱
     */
    public List<ParsedFeedEntry> parse(String rssUrl) {
        try {
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(URI.create(rssUrl).toURL()));

            return feed.getEntries().stream()
                    .map(this::toFeedEntry)
                    .toList();
        } catch (Exception e) {
            log.error("RSS 파싱 실패: url={}, error={}", rssUrl, e.getMessage());
            return Collections.emptyList();
        }
    }

    private ParsedFeedEntry toFeedEntry(SyndEntry entry) {
        return new ParsedFeedEntry(
                truncate(entry.getTitle(), 500),
                truncate(entry.getLink(), 2048),
                extractDescription(entry),
                truncate(entry.getAuthor(), 255),
                extractPublishedAt(entry),
                extractGuid(entry)
        );
    }

    private String extractDescription(SyndEntry entry) {
        if (entry.getDescription() != null) {
            return entry.getDescription().getValue();
        }
        if (entry.getContents() != null && !entry.getContents().isEmpty()) {
            return entry.getContents().get(0).getValue();
        }
        return null;
    }

    private String extractGuid(SyndEntry entry) {
        // guid가 있으면 사용, 없으면 link 사용
        if (entry.getUri() != null && !entry.getUri().isBlank()) {
            return truncate(entry.getUri(), 2048);
        }
        return truncate(entry.getLink(), 2048);
    }

    /**
     * 발행일자 추출
     * Atom 피드의 경우 published가 없으면 updated 사용
     */
    private Instant extractPublishedAt(SyndEntry entry) {
        Date publishedDate = entry.getPublishedDate();
        if (publishedDate != null) {
            return publishedDate.toInstant();
        }
        Date updatedDate = entry.getUpdatedDate();
        if (updatedDate != null) {
            return updatedDate.toInstant();
        }
        return null;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
