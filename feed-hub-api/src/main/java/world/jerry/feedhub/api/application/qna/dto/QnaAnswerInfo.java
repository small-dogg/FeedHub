package world.jerry.feedhub.api.application.qna.dto;

import world.jerry.feedhub.api.domain.qna.QnaAnswer;

import java.time.Instant;

/**
 * QnA 답변 DTO
 */
public record QnaAnswerInfo(
        Long id,
        Long memberId,
        String memberNickname,
        String content,
        Instant createdAt
) {
    public static QnaAnswerInfo from(QnaAnswer answer, String memberNickname) {
        return new QnaAnswerInfo(
                answer.getId(),
                answer.getMemberId(),
                memberNickname,
                answer.getContent(),
                answer.getCreatedAt()
        );
    }
}
