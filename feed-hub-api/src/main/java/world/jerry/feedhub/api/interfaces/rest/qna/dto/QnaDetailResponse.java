package world.jerry.feedhub.api.interfaces.rest.qna.dto;

import world.jerry.feedhub.api.application.qna.dto.QnaAnswerInfo;
import world.jerry.feedhub.api.application.qna.dto.QnaDetailInfo;

import java.time.Instant;
import java.util.List;

public record QnaDetailResponse(
        Long id,
        String memberNickname,
        String type,
        String title,
        String content,
        boolean isSecret,
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<AnswerResponse> answers
) {
    public static QnaDetailResponse from(QnaDetailInfo info) {
        List<AnswerResponse> answerResponses = info.answers().stream()
                .map(AnswerResponse::from)
                .toList();

        return new QnaDetailResponse(
                info.id(),
                info.memberNickname(),
                info.type().name(),
                info.title(),
                info.content(),
                info.isSecret(),
                info.status().name(),
                info.createdAt(),
                info.updatedAt(),
                answerResponses
        );
    }

    public record AnswerResponse(
            Long id,
            String memberNickname,
            String content,
            Instant createdAt
    ) {
        public static AnswerResponse from(QnaAnswerInfo info) {
            return new AnswerResponse(
                    info.id(),
                    info.memberNickname(),
                    info.content(),
                    info.createdAt()
            );
        }
    }
}
