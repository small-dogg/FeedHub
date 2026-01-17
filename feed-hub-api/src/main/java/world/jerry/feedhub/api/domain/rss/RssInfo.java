package world.jerry.feedhub.api.domain.rss;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    public RssInfo(String blogName, String author, String rssUrl, String siteUrl, String language) {
        this.blogName = blogName;
        this.author = author;
        this.rssUrl = rssUrl;
        this.siteUrl = siteUrl;
        this.language = language;
        this.createdAt = Instant.now();
    }

    public void updateLastSyncAt(Instant syncTime) {
        this.lastSyncAt = syncTime;
    }
}
