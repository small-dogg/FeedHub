package world.jerry.feedhub.api.domain.feed;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "feed_entry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rss_info_id", nullable = false)
    private Long rssInfoId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "link", nullable = false, length = 2048)
    private String link;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "author")
    private String author;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "guid", length = 2048)
    private String guid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public FeedEntry(Long rssInfoId, String title, String link, String description,
                     String author, Instant publishedAt, String guid) {
        this.rssInfoId = rssInfoId;
        this.title = title;
        this.link = link;
        this.description = description;
        this.author = author;
        this.publishedAt = publishedAt;
        this.guid = guid;
        this.createdAt = Instant.now();
    }
}
