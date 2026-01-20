package world.jerry.feedhub.api.application.qna.dto;

import world.jerry.feedhub.api.domain.qna.QnaType;

public record CreateQnaCommand(
        Long memberId,
        QnaType type,
        String title,
        String content,
        boolean isSecret
) {
}
