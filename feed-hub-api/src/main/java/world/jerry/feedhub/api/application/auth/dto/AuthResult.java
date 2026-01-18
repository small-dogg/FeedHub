package world.jerry.feedhub.api.application.auth.dto;

public record AuthResult(
        String accessToken,
        MemberInfo member
) {
}
