package world.jerry.feedhub.api.application.qna.dto;

import world.jerry.feedhub.api.domain.qna.Qna;
import world.jerry.feedhub.api.domain.qna.QnaStatus;
import world.jerry.feedhub.api.domain.qna.QnaType;

import java.time.Instant;

/**
 * QnA 목록 조회용 DTO
 */
public record QnaInfo(
        Long id,
        Long memberId,
        String memberNickname,
        QnaType type,
        String title,
        boolean isSecret,
        QnaStatus status,
        Instant createdAt,
        long answerCount
) {
    public static QnaInfo from(Qna qna, String memberNickname, long answerCount) {
        return new QnaInfo(
                qna.getId(),
                qna.getMemberId(),
                memberNickname,
                qna.getType(),
                qna.getTitle(),
                qna.isSecret(),
                qna.getStatus(),
                qna.getCreatedAt(),
                answerCount
        );
    }
}
