package world.jerry.feedhub.api.domain.qna;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * QnA 답변
 */
@Entity
@Table(name = "qna_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "qna_id", nullable = false)
    private Long qnaId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public QnaAnswer(Long qnaId, Long memberId, String content) {
        this.qnaId = qnaId;
        this.memberId = memberId;
        this.content = content;
        this.createdAt = Instant.now();
    }
}
