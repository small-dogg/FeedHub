package world.jerry.feedhub.api.domain.feed;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 회원의 피드 읽음 기록
 */
@Entity
@Table(name = "member_feed_read")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberFeedRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "feed_entry_id", nullable = false)
    private Long feedEntryId;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    public MemberFeedRead(Long memberId, Long feedEntryId) {
        this.memberId = memberId;
        this.feedEntryId = feedEntryId;
        this.readAt = Instant.now();
    }
}
