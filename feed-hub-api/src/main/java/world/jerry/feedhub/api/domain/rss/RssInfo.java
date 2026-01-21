package world.jerry.feedhub.api.domain.rss;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import world.jerry.feedhub.common.domain.BlogType;

import java.time.Instant;

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

    public RssInfo(String blogName, String author, String rssUrl, String siteUrl, String crawlUrl, String language, BlogType blogType) {
        this.blogName = blogName;
        this.author = author;
        this.rssUrl = rssUrl;
        this.siteUrl = siteUrl;
        this.crawlUrl = crawlUrl;
        this.language = language;
        this.blogType = blogType != null ? blogType : BlogType.UNKNOWN;
        this.createdAt = Instant.now();
    }

    public void updateLastSyncAt(Instant syncTime) {
        this.lastSyncAt = syncTime;
    }

    public void updateBlogType(BlogType blogType) {
        this.blogType = blogType;
    }

    public void update(String blogName, String author, String siteUrl, String crawlUrl, String language, BlogType blogType) {
        if (blogName != null && !blogName.isBlank()) {
            this.blogName = blogName;
        }
        this.author = author;
        this.siteUrl = siteUrl;
        this.crawlUrl = crawlUrl;
        this.language = language;
        if (blogType != null) {
            this.blogType = blogType;
        }
    }
}
