package world.jerry.feedhub.api.application.auth.dto;

public record MemberInfo(
        Long id,
        String email,
        String nickname
) {
}
