package world.jerry.feedhub.api.interfaces.rest.qna.dto;

import world.jerry.feedhub.api.application.qna.dto.QnaInfo;

import java.time.Instant;

public record QnaResponse(
        Long id,
        String memberNickname,
        String type,
        String title,
        boolean isSecret,
        String status,
        Instant createdAt,
        long answerCount
) {
    public static QnaResponse from(QnaInfo info) {
        return new QnaResponse(
                info.id(),
                info.memberNickname(),
                info.type().name(),
                info.title(),
                info.isSecret(),
                info.status().name(),
                info.createdAt(),
                info.answerCount()
        );
    }
}
