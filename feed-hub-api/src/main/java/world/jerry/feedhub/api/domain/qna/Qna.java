package world.jerry.feedhub.api.domain.qna;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * QnA 질문
 */
@Entity
@Table(name = "qna")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Qna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private QnaType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "is_secret", nullable = false)
    private boolean isSecret;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QnaStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Qna(Long memberId, QnaType type, String title, String content, boolean isSecret) {
        this.memberId = memberId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.isSecret = isSecret;
        this.status = QnaStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void markAsAnswered() {
        this.status = QnaStatus.ANSWERED;
        this.updatedAt = Instant.now();
    }

    public void close() {
        this.status = QnaStatus.CLOSED;
        this.updatedAt = Instant.now();
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }
}
