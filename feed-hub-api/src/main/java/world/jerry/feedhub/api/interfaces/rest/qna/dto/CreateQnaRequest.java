package world.jerry.feedhub.api.interfaces.rest.qna.dto;

import world.jerry.feedhub.api.application.qna.dto.CreateQnaCommand;
import world.jerry.feedhub.api.domain.qna.QnaType;

public record CreateQnaRequest(
        String type,
        String title,
        String content,
        boolean isSecret
) {
    public CreateQnaCommand toCommand(Long memberId) {
        return new CreateQnaCommand(
                memberId,
                QnaType.valueOf(type),
                title,
                content,
                isSecret
        );
    }
}
