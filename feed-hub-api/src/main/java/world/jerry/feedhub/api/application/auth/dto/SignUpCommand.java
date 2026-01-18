package world.jerry.feedhub.api.application.auth.dto;

public record SignUpCommand(
        String email,
        String password,
        String nickname
) {
}
