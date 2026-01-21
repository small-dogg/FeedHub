package world.jerry.feedhub.collector.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import world.jerry.feedhub.common.domain.BlogType;

import java.time.Instant;

/**
 * RSS source information entity (read-only in Collector)
 */
@Entity
@Table(name = "rss_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RssInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blog_name", nullable = false)
    private String blogName;

    @Column(name = "author")
    private String author;

    @Column(name = "rss_url", nullable = false, unique = true, length = 2048)
    private String rssUrl;

    @Column(name = "site_url", length = 2048)
    private String siteUrl;

    @Column(name = "crawl_url", length = 2048)
    private String crawlUrl;

    @Column(name = "language", length = 10)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(name = "blog_type", length = 20)
    private BlogType blogType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    public void updateLastSyncAt(Instant syncTime) {
        this.lastSyncAt = syncTime;
    }
}
