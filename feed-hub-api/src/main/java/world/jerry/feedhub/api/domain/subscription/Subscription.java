package world.jerry.feedhub.api.domain.subscription;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import world.jerry.feedhub.api.domain.member.Member;
import world.jerry.feedhub.api.domain.rss.RssInfo;

import java.time.Instant;

@Entity
@Table(name = "subscription", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "member_id", "rss_info_id" })
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rss_info_id", nullable = false)
    private RssInfo rssInfo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Subscription(Member member, RssInfo rssInfo) {
        this.member = member;
        this.rssInfo = rssInfo;
        this.createdAt = Instant.now();
    }
}
