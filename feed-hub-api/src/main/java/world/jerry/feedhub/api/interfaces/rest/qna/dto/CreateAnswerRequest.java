package world.jerry.feedhub.api.interfaces.rest.qna.dto;

import world.jerry.feedhub.api.application.qna.dto.CreateAnswerCommand;

public record CreateAnswerRequest(
        String content
) {
    public CreateAnswerCommand toCommand(Long memberId) {
        return new CreateAnswerCommand(memberId, content);
    }
}
