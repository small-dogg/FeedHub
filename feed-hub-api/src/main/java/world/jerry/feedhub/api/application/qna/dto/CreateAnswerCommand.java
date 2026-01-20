package world.jerry.feedhub.api.application.qna.dto;

public record CreateAnswerCommand(
        Long memberId,
        String content
) {
}
