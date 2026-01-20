package world.jerry.feedhub.api.domain.feed;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 회원의 피드 좋아요 기록
 */
@Entity
@Table(name = "feed_like",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "feed_entry_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "feed_entry_id", nullable = false)
    private Long feedEntryId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public FeedLike(Long memberId, Long feedEntryId) {
        this.memberId = memberId;
        this.feedEntryId = feedEntryId;
        this.createdAt = Instant.now();
    }
}
