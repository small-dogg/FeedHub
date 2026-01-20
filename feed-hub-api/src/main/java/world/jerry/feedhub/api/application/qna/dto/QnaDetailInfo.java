package world.jerry.feedhub.api.application.qna.dto;

import world.jerry.feedhub.api.domain.qna.Qna;
import world.jerry.feedhub.api.domain.qna.QnaStatus;
import world.jerry.feedhub.api.domain.qna.QnaType;

import java.time.Instant;
import java.util.List;

/**
 * QnA 상세 조회용 DTO
 */
public record QnaDetailInfo(
        Long id,
        Long memberId,
        String memberNickname,
        QnaType type,
        String title,
        String content,
        boolean isSecret,
        QnaStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<QnaAnswerInfo> answers
) {
    public static QnaDetailInfo from(Qna qna, String memberNickname, List<QnaAnswerInfo> answers) {
        return new QnaDetailInfo(
                qna.getId(),
                qna.getMemberId(),
                memberNickname,
                qna.getType(),
                qna.getTitle(),
                qna.getContent(),
                qna.isSecret(),
                qna.getStatus(),
                qna.getCreatedAt(),
                qna.getUpdatedAt(),
                answers
        );
    }
}
